package slimly

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Реестр событий аналитики — андроидная замена `Tools/AnalyticsRegistry.swift`.
 *
 * Источник правды — код: имена событий живут в аналитике модуля, а таблица
 * собирается из них. Поэтому `ANALYTICS.md` руками не правят: следующий
 * запуск всё равно перепишет файл.
 *
 * На iOS это отдельная команда, здесь — задача сборки: у Gradle уже есть
 * и обход исходников, и место, куда повесить проверку.
 */
abstract class AnalyticsRegistryTask : DefaultTask() {
    /**
     * Исходники всех модулей. Именно файлы, а не корневая папка: та тянет
     * за собой `build/` и `.gradle/`, и задача переставала бы быть свежей
     * после каждой сборки.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    /**
     * Реестр соседней платформы. Его наличие не обязательно: андроидный
     * репозиторий должен собираться и сам по себе.
     */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val iosRegistry: RegularFileProperty

    @get:OutputFile
    abstract val registry: RegularFileProperty

    /** Корень репозитория: путь в заголовке таблицы пишется относительно него. */
    @get:Internal
    abstract val projectRoot: DirectoryProperty

    /** `true` — только проверить свежесть, не переписывая файл. */
    @get:Input
    abstract val checkOnly: Property<Boolean>

    @TaskAction
    fun run() {
        val root = projectRoot.get().asFile
        val catalogs = sources.files
            .sorted()
            .mapNotNull { catalog(it, root) }
            .sortedBy { it.scope }

        problems(catalogs).takeIf { it.isNotEmpty() }?.let { found ->
            throw GradleException(found.joinToString(separator = "\n") { "Реестр событий: $it" })
        }

        val text = markdown(catalogs, divergence(catalogs))
        val file = registry.get().asFile
        val count = catalogs.sumOf { it.events.size }

        if (checkOnly.get()) {
            if (file.takeIf { it.exists() }?.readText() != text) {
                throw GradleException("ANALYTICS.md устарел — выполни ./gradlew generateAnalyticsRegistry")
            }
            logger.lifecycle("ANALYTICS.md актуален: событий — $count")
            return
        }

        file.parentFile.mkdirs()
        file.writeText(text)
        logger.lifecycle("ANALYTICS.md собран: событий — $count")
    }

    /**
     * Расхождения с iOS. Не ошибка сборки: платформы законно отличаются —
     * на Android нет своего выбора языка, а инструменты приезжают по фазам.
     * Задача только называет разницу, решать её — человеку.
     */
    private fun divergence(catalogs: List<Catalog>): List<String> {
        val file = iosRegistry.orNull?.asFile ?: return emptyList()
        if (!file.exists()) return emptyList()

        val theirs = IOS_EVENT.findAll(file.readText()).map { it.groupValues[1] }.toSet()
        val ours = catalogs.flatMap { catalog -> catalog.events.map { it.fullName } }.toSet()

        return ((theirs - ours).map { "`$it` — только на iOS" } + (ours - theirs).map { "`$it` — только на Android" })
            .sorted()
    }

    private companion object {
        val IOS_EVENT = Regex("""^\|\s*`([a-z0-9_]+)`\s*\|""", RegexOption.MULTILINE)
    }
}

/** Событие: имя, параметры и одна строка о том, что оно значит. */
internal data class AnalyticsEventEntry(
    val scope: String,
    val name: String,
    val parameters: List<String>,
    val summary: String,
) {
    val fullName: String get() = "${scope}_$name"
}

internal data class Catalog(val scope: String, val file: String, val events: List<AnalyticsEventEntry>)

private val SCOPE = Regex("""override\s+val\s+scope\s*:?\s*\w*\s*=\s*"([^"]+)"""")
private val TRACK = Regex("""track\(\s*(?:name\s*=\s*)?"([^"]+)"""")

/**
 * Ключ словаря параметров: строка, за которой идёт `to`. Значение при этом
 * может быть любым — и вызовом, и полем, — поэтому в шаблоне его нет.
 */
private val PARAMETER = Regex(""""([^"]+)"\s+to\s""")

/**
 * Каталог событий одного модуля. `null` — файл не аналитика модуля:
 * либо не реализует `FeatureAnalytics`, либо не объявляет префикса.
 */
internal fun catalog(file: File, root: File): Catalog? {
    val text = file.readText()
    if (!text.contains("FeatureAnalytics")) return null
    val scope = SCOPE.find(text)?.groupValues?.get(1) ?: return null

    val lines = text.lines()
    val events = mutableListOf<AnalyticsEventEntry>()
    var documentation = mutableListOf<String>()
    var pending = DASH

    lines.forEachIndexed { index, raw ->
        val line = raw.trim()

        when {
            // Однострочный комментарий: открытие и закрытие на одной строке,
            // и по общим веткам он разошёлся бы на два разных случая.
            line.startsWith("/**") && line.endsWith("*/") ->
                documentation = mutableListOf(line.removePrefix("/**").removeSuffix("*/").trim())

            line.startsWith("/**") -> documentation = mutableListOf()
            line.startsWith("*/") -> Unit
            line.startsWith("*") -> documentation.add(line.removePrefix("*").trim())

            // Документация относится к ближайшей функции под собой; всё
            // остальное её сбрасывает, иначе комментарий уехал бы к чужому
            // событию.
            line.startsWith("fun ") -> pending = summary(documentation)
            line.isNotEmpty() -> documentation = mutableListOf()
        }

        if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) return@forEachIndexed
        val call = call(lines, index) ?: return@forEachIndexed
        val name = TRACK.find(call)?.groupValues?.get(1) ?: return@forEachIndexed

        events.add(
            AnalyticsEventEntry(
                scope = scope,
                name = name,
                parameters = PARAMETER.findAll(call).map { it.groupValues[1] }.toList().sorted(),
                summary = pending,
            ),
        )
    }

    if (events.isEmpty()) return null
    return Catalog(
        scope = scope,
        file = file.relativeTo(root).invariantSeparatorsPath,
        events = events.sortedBy { it.name },
    )
}

/**
 * Текст вызова целиком: словарь параметров часто переносят на другие строки,
 * и по одной строке его не собрать.
 */
private fun call(lines: List<String>, index: Int): String? {
    val start = lines[index].indexOf("track(").takeIf { it >= 0 } ?: return null

    val text = StringBuilder()
    var depth = 0
    var opened = false

    for (line in index until lines.size) {
        val piece = if (line == index) lines[line].substring(start) else lines[line]
        for (character in piece) {
            text.append(character)
            if (character == '(') {
                depth += 1
                opened = true
            }
            if (character == ')') depth -= 1
            if (opened && depth == 0) return text.toString()
        }
        text.append(' ')
    }

    return null
}

/**
 * Первое предложение документации: в таблице нужна одна строка, а в коде
 * у события обычно развёрнутый комментарий про «почему».
 */
private fun summary(documentation: List<String>): String {
    val text = documentation.joinToString(separator = " ").trim()
    if (text.isEmpty()) return DASH

    // Вертикальная черта развалила бы строку таблицы на две ячейки.
    val escaped = text.replace("|", "\\|")
    val stop = escaped.indexOf(". ")
    return if (stop < 0) escaped else escaped.substring(0, stop) + "."
}

/**
 * Ограничения Firebase: имя длиннее сорока символов или с посторонним
 * символом событие не отбрасывает с ошибкой, а молча теряет.
 */
internal fun problems(catalogs: List<Catalog>): List<String> {
    val problems = mutableListOf<String>()
    val seen = mutableMapOf<String, String>()

    fun valid(name: String): Boolean = name.firstOrNull()?.isLetter() == true &&
        name.all { it.code < ASCII_LIMIT && (it.isLetter() && it.isLowerCase() || it.isDigit() || it == '_') }

    catalogs.forEach { catalog ->
        catalog.events.forEach { event ->
            seen[event.fullName]?.let { other ->
                problems.add("событие ${event.fullName} объявлено дважды: $other и ${catalog.file}")
            }
            seen[event.fullName] = catalog.file

            if (event.fullName.length > NAME_LIMIT) {
                problems.add("имя ${event.fullName} длиннее $NAME_LIMIT символов (${catalog.file})")
            }
            if (!valid(event.fullName)) {
                problems.add("имя ${event.fullName} не из строчной латиницы, цифр и подчёркиваний (${catalog.file})")
            }
            if (event.parameters.size > PARAMETER_LIMIT) {
                problems.add("у ${event.fullName} больше $PARAMETER_LIMIT параметров (${catalog.file})")
            }
            event.parameters
                .filter { !valid(it) || it.length > NAME_LIMIT }
                .forEach { problems.add("параметр $it события ${event.fullName} Firebase не примет (${catalog.file})") }
        }
    }

    return problems.sorted()
}

internal fun markdown(catalogs: List<Catalog>, divergence: List<String>): String = buildString {
    appendLine("# События аналитики")
    appendLine()
    appendLine("<!-- Файл собран автоматически: `./gradlew generateAnalyticsRegistry`.")
    appendLine("     Руками не правят — источник правды в коде. -->")
    appendLine()
    appendLine("Имя события целиком — это префикс модуля плюс имя из `track(...)`.")
    appendLine()

    catalogs.forEach { catalog ->
        appendLine()
        appendLine("## ${catalog.scope} · `${catalog.file}`")
        appendLine()
        appendLine("| Событие | Параметры | Что значит |")
        appendLine("| --- | --- | --- |")
        catalog.events.forEach { event ->
            val parameters = if (event.parameters.isEmpty()) DASH else event.parameters.joinToString { "`$it`" }
            appendLine("| `${event.fullName}` | $parameters | ${event.summary} |")
        }
    }

    if (divergence.isEmpty()) return@buildString

    appendLine()
    appendLine("## Расхождения с iOS")
    appendLine()
    appendLine("Не ошибка сама по себе: инструменты приезжают по фазам, а часть")
    appendLine("экранов у платформ разная. Но каждая строка здесь — повод")
    appendLine("проверить, что событие потеряно намеренно.")
    appendLine()
    divergence.forEach { appendLine("- $it") }
}

private const val DASH = "—"
private const val NAME_LIMIT = 40
private const val PARAMETER_LIMIT = 25
private const val ASCII_LIMIT = 128
