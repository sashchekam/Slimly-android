#!/usr/bin/env python3
"""Переносит переводы из иосных каталогов `.xcstrings` в андроидные `strings.xml`.

    python3 tools/l10n/import-catalogs.py            # переписать values-*/strings.xml
    python3 tools/l10n/import-catalogs.py --report   # показать, чего не хватает

Конвертер одноразовый: 393 ключа на десяти языках уже переведены на iOS,
и переписывать их руками незачем. Дальше строки живут своей жизнью в обоих
репозиториях — генератора вроде иосного `L10nGenerator` здесь нет и не будет:
`R.string` компилятор проверяет сам.

Ключи у платформ разошлись («picker.deniedNote» против «picker_denied_hint»),
поэтому сопоставляем не по именам, а по английскому тексту: он и есть исходный
язык каталогов. Английский на Android остаётся в `values/strings.xml` как был —
файл источника скрипт не трогает.

Тексты, которых на iOS нет вовсе (уведомление сервиса, слово «галерея» вместо
«Фото»), берутся из `tools/l10n/android-only.json` — он заполнен руками.
Значение `null` там означает «одинаково на всех языках»: «%1$d%%» и «MP4 ·
H.264 / HEVC» переводить нечего, и девяти копий одной строки быть не должно.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
IOS = ROOT.parent / "ios"
EXTRA = Path(__file__).with_name("android-only.json")

# Каталог модуля ищем сначала свой, потом любой: часть строк переехала между
# модулями (подписи инструментов у нас в :shortcuts, на iOS — в HomeFeature).
MODULES = {
    "core": "Modules/Core/SlimlyCore",
    "designsystem": "Modules/Core/SlimlyDesignSystem",
    "navigation": "Modules/Core/SlimlyNavigation",
    "mediakit": "Modules/Core/SlimlyMediaKit",
    "mediaflow": "Modules/Core/SlimlyMediaFlow",
    "shortcuts": "Modules/Core/SlimlyShortcuts",
    "widget": "Widget",
    "app": "App",
    "feature/home/impl": "Modules/Features/HomeFeature",
    "feature/profile/impl": "Modules/Features/ProfileFeature",
    "feature/compress/impl": "Modules/Features/CompressFeature",
    "feature/convert/impl": "Modules/Features/ConvertFeature",
    "feature/onboarding/impl": "Modules/Features/OnboardingFeature",
    "feature/audio/impl": "Modules/Features/AudioFeature",
    "feature/scan/impl": "Modules/Features/ScanFeature",
    "feature/debug/impl": "Modules/Features/DebugFeature",
}

# Языки и папки ресурсов. Теги BCP-47 («b+pt+BR») вместо старых «pt-rBR»:
# в `locales_config.xml` стоят ровно такие же, и гадать о соответствии
# не приходится.
LANGUAGES = {
    "de": "values-de",
    "es": "values-es",
    "fr": "values-fr",
    "it": "values-it",
    "ja": "values-ja",
    "ko": "values-ko",
    "pt-BR": "values-b+pt+BR",
    "ru": "values-ru",
    "zh-Hans": "values-b+zh+Hans",
}

SPECIFIER = re.compile(r"%(?:(\d+)\$)?(@|lld|d|s)")


def canonical(text: str, fold: bool = False) -> str:
    """Текст без того, что у платформ различается заведомо.

    Плейсхолдеры сводим к номерным ячейкам, типографику — к простым знакам:
    один и тот же перевод на iOS набран «ёлочками», а у нас мог приехать
    прямыми кавычками.

    Регистр по умолчанию сохраняем: «All time» и «all time» — это две разные
    строки каталога, одна для заголовка, другая внутри фразы. Складывать их
    в одну можно только вторым заходом, когда точного совпадения не нашлось.
    """
    counter = [0]

    def slot(match: re.Match[str]) -> str:
        counter[0] += 1
        return "{%s}" % (match.group(1) or counter[0])

    text = SPECIFIER.sub(slot, text)
    for source, target in ((" ", " "), ("’", "'"), ("‘", "'"),
                           ("“", '"'), ("”", '"'), ("—", "-"), ("–", "-")):
        text = text.replace(source, target)
    text = re.sub(r"\s+", " ", text).strip()
    return text.lower() if fold else text


def android_format(text: str) -> str:
    """Иосные плейсхолдеры в андроидные: `%@` → `%1$s`, `%lld` → `%2$d`."""
    counter = [0]

    def convert(match: re.Match[str]) -> str:
        counter[0] += 1
        index = match.group(1) or counter[0]
        letter = "d" if match.group(2) in ("lld", "d") else "s"
        return f"%{index}${letter}"

    return SPECIFIER.sub(convert, text)


def escape(text: str) -> str:
    """Экранирование по правилам ресурсов Android.

    Апостроф и кавычка в `strings.xml` — служебные знаки: без обратной косой
    ресурс просто не соберётся. Одиночный процент безопасен, пока в строке
    есть хоть один плейсхолдер, — а он есть везде, где процент встречается.
    """
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return text.replace("'", "\\'").replace('"', '\\"')


def unit(node: dict) -> str | None:
    return node.get("stringUnit", {}).get("value")


def plural_forms(node: dict) -> dict[str, str]:
    """Формы множественного числа одного языка.

    На iOS их записывают двумя способами: прямо в `variations` — когда вся
    строка склоняется целиком, — и через `substitutions`, когда склоняется
    одно слово внутри фразы. Второй способ нам подходит, только если фраза
    из этого слова и состоит: андроидные `plurals` подставляют строку целиком.
    """
    if cases := node.get("variations", {}).get("plural"):
        return {case: unit(body) for case, body in cases.items() if unit(body)}

    template = (unit(node) or "").strip()
    match = re.fullmatch(r"%#@(\w+)@", template)
    if not match:
        return {}

    substitution = node.get("substitutions", {}).get(match.group(1), {})
    cases = substitution.get("variations", {}).get("plural", {})
    return {case: unit(body) for case, body in cases.items() if unit(body)}


def load_ios() -> tuple[dict, dict]:
    """Два указателя: «английский текст → переводы» и то же самое по модулям."""
    plain: dict[str, dict[str, str]] = {}
    plural: dict[str, dict[str, dict[str, str]]] = {}

    for path in sorted(IOS.rglob("*.xcstrings")):
        if "/Derived/" in str(path):
            continue
        catalog = json.loads(path.read_text(encoding="utf-8"))

        for key, entry in catalog.get("strings", {}).items():
            localizations = entry.get("localizations", {})
            english = localizations.get("en", {})

            # Формы множественного числа проверяем первыми: у строки
            # с подстановкой есть и обычное значение — шаблон `%#@count@`,
            # и как простую строку его брать нельзя.
            forms = {lang: plural_forms(node) for lang, node in localizations.items()}
            forms = {lang: cases for lang, cases in forms.items() if cases}
            other = (forms.get("en") or {}).get("other")
            if other:
                for key in (canonical(other), canonical(other, fold=True)):
                    plural.setdefault(key, {}).update(forms)
                continue

            if value := unit(english):
                translations = {lang: unit(node) for lang, node in localizations.items() if unit(node)}
                for key in (canonical(value), canonical(value, fold=True)):
                    plain.setdefault(key, {}).update(translations)

    return plain, plural


def lookup(index: dict, english: str) -> dict | None:
    """Сначала точное совпадение, потом без учёта регистра."""
    return index.get(canonical(english)) or index.get(canonical(english, fold=True))


def load_extra() -> dict:
    if not EXTRA.exists():
        return {}
    return json.loads(EXTRA.read_text(encoding="utf-8"))


def strings_of(path: Path) -> ET.ElementTree:
    return ET.parse(path)


def text_of(node: ET.Element) -> str:
    """Текст ресурса как он лежит в файле, вместе с экранированием."""
    return "".join(node.itertext())


def render(module: str, language: str, items: list[tuple[str, object]]) -> str:
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<!--",
        f"  Перевод модуля на «{language}». Файл собран конвертером:",
        "  python3 tools/l10n/import-catalogs.py. Английский лежит",
        "  в values/strings.xml — правят там, а перевод приезжает сюда.",
        "-->",
        "<resources>",
        "",
    ]
    for name, value in items:
        if isinstance(value, dict):
            lines.append(f'    <plurals name="{name}">')
            for case in ("zero", "one", "two", "few", "many", "other"):
                if case in value:
                    lines.append(f'        <item quantity="{case}">{escape(value[case])}</item>')
            lines.append("    </plurals>")
        else:
            lines.append(f'    <string name="{name}">{escape(value)}</string>')
    lines += ["", "</resources>", ""]
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", action="store_true", help="только показать, чего не хватает")
    arguments = parser.parse_args()

    plain, plural = load_ios()
    extra = load_extra()
    missing: list[str] = []
    written = 0

    for module in MODULES:
        source = ROOT / module / "src/main/res/values/strings.xml"
        if not source.exists():
            continue

        tree = strings_of(source)
        collected: dict[str, list[tuple[str, object]]] = {lang: [] for lang in LANGUAGES}

        for node in tree.getroot():
            name = node.get("name")
            key = f"{module}:{name}"
            if key in extra and extra[key] is None:
                continue
            own = extra.get(key)

            if node.tag == "string":
                english = text_of(node)
                translations = own or lookup(plain, english)
                if not translations:
                    missing.append(f"{module} · {name} · {english}")
                    continue
                for lang in LANGUAGES:
                    if value := translations.get(lang):
                        collected[lang].append((name, android_format(value)))

            elif node.tag == "plurals":
                cases = {item.get("quantity"): text_of(item) for item in node}
                translations = own or lookup(plural, cases.get("other", ""))
                if not translations:
                    missing.append(f"{module} · {name} · {cases.get('other', '')}")
                    continue
                for lang in LANGUAGES:
                    if forms := translations.get(lang):
                        collected[lang].append((name, {k: android_format(v) for k, v in forms.items()}))

        if arguments.report:
            continue

        for lang, folder in LANGUAGES.items():
            items = collected[lang]
            target = ROOT / module / "src/main/res" / folder / "strings.xml"
            if not items:
                target.unlink(missing_ok=True)
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(render(module, lang, items), encoding="utf-8")
            written += 1

    if missing:
        print(f"Без перевода — {len(missing)}:", file=sys.stderr)
        for line in missing:
            print(f"  {line}", file=sys.stderr)

    if not arguments.report:
        print(f"Собрано файлов: {written}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
