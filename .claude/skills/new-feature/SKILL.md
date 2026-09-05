---
name: new-feature
description: Создать новый фичевый модуль в Slimly для Android — пару :api и :impl, запись в реестре SlimlyModule, маршруты и точку сборки. Использовать, когда просят добавить фичу, новый экранный модуль или новый раздел приложения (инструмент, профиль, онбординг, пейволл и т.п.).
---

# Новая фича

Фича = два модуля Gradle: тонкий `:feature:<name>:api` (только маршруты,
без UI) и `:feature:<name>:impl` (экраны, вьюмодели, точка сборки).
Эталон — `feature/home`.

## Шаги

### 1. Запись в реестре

`build-logic/convention/src/main/kotlin/slimly/SlimlyModule.kt` — добавить
**два** элемента перечисления и разобрать их во всех `when`: `kind`
и `dependencies`.

```kotlin
<Name>Api(":feature:<name>:api"),
<Name>(":feature:<name>:impl"),
```

- `<Name>Api` → `Kind.FEATURE_API`, зависимости `listOf(Core)`
- `<Name>` → `Kind.FEATURE`, зависимости `listOf(Core, DesignSystem, <Name>Api)`
  плюс `:api` тех фич, чьи экраны она открывает
- `:app` → добавить обе записи в его список зависимостей

Порядок объявления — порядок модулей в сборке. Пространство имён считается
по пути само, объявлять его не нужно.

### 2. Файлы

```
feature/<name>/api/build.gradle.kts
feature/<name>/api/src/main/kotlin/com/slimly/feature/<name>/api/<Name>Route.kt
feature/<name>/impl/build.gradle.kts
feature/<name>/impl/src/main/kotlin/com/slimly/feature/<name>/<Name>Screen.kt
feature/<name>/impl/src/main/kotlin/com/slimly/feature/<name>/<Name>ViewModel.kt
```

Оба `build.gradle.kts` — одна строка, без блока `dependencies`:

```kotlin
plugins {
    id("slimly.android.library")
}
```

Зависимости раздаёт реестр. Дописанная руками связь валит `checkModuleGraph`.

`<Name>Route.kt` в `:api` — публичный вход в фичу: маршруты и ничего больше.
`<Name>Screen.kt` — Composable, принимает готовую вьюмодель.
Вьюмодель держит состояние в `mutableStateOf` с `private set` и получает
роутер и сервисы параметрами конструктора.

Если фича — инструмент обработки файлов, не пиши экраны заново: общие шаги
(выбор, формат, обработка, итог) живут в `:mediaflow` — фиче остаётся конфиг
шагов и то, что есть только у неё.

### 3. Проверка

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug
```

Сборка сама прогонит `checkModuleGraph`, detekt и ktlint.

## Чего не делать

- Не зависеть от чужого `:impl` — только от `:api`. Реестр не пропустит,
  а обход мимо реестра поймает `checkModuleGraph`.
- Не писать `implementation(project(...))` в модуле.
- Не добавлять модуль в `settings.gradle.kts` — состав сборки берётся
  из реестра.
- Не класть UI, вьюмодели и сервисы в `:api` — там живут только маршруты.
