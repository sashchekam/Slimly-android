package com.slimly.feature.debug

import android.content.res.Resources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.analytics.AnalyticsLog
import com.slimly.core.analytics.AnalyticsRecord
import com.slimly.navigation.Router
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Строка списка — одна отправка. */
data class DebugAnalyticsEntry(
    val id: UUID,
    val name: String,
    val time: String,
    /** Параметры в одну строку — то, что видно, пока строка не раскрыта. */
    val summary: String,
    /**
     * Пустой список значит «раскрывать нечего»: у события без параметров
     * и у свойства всё уже написано в самой строке.
     */
    val parameters: List<DebugAnalyticsParameter>,
) {
    val isExpandable: Boolean
        get() = parameters.isNotEmpty()
}

data class DebugAnalyticsParameter(val name: String, val value: String)

/**
 * Что приложение отправило в аналитику с этого запуска.
 *
 * Экран отвечает на вопрос, на который веб-консоль отвечает через сутки:
 * ушло ли событие вообще и то ли в нём лежит. Ошибаются обычно в параметрах,
 * поэтому они здесь рядом с именем, а не в отдельном отчёте.
 */
class DebugAnalyticsViewModel(
    private val log: AnalyticsLog,
    private val clipboard: DebugClipboard,
    private val resources: Resources,
    private val router: Router,
    private val scope: CoroutineScope,
) {
    var toast: String? by mutableStateOf(null)
        private set

    /** Выбранный префикс модуля; `null` — показываем всё. */
    var scopeFilter: String? by mutableStateOf(null)
        private set

    /**
     * Раскрытые строки. Параметры всех событий сразу превращают список
     * в стену текста, по которой уже не видно порядка отправки.
     */
    private var expanded: Set<UUID> by mutableStateOf(emptySet())

    private var toastJob: Job? = null

    val entries: List<DebugAnalyticsEntry>
        get() = visible.map(::entry)

    /**
     * Префиксы, которые в журнале есть. Пока префикс один, фильтра нет:
     * чипы предлагали бы выбор из единственного варианта.
     */
    val scopes: List<String>
        get() {
            val names = log.records.map(::scopeOf).distinct()
            return if (names.size > 1) names.sorted() else emptyList()
        }

    val isEmpty: Boolean
        get() = log.records.isEmpty()

    val capacity: Int
        get() = log.capacity

    fun isSelected(scope: String?): Boolean = scopeFilter == scope

    fun select(scope: String?) {
        scopeFilter = scope
    }

    /**
     * Раскрытой считается только строка, у которой есть что показать:
     * иначе у свойства пропала бы единственная строка с его значением.
     */
    fun isExpanded(entry: DebugAnalyticsEntry): Boolean = entry.isExpandable && entry.id in expanded

    fun toggle(entry: DebugAnalyticsEntry) {
        if (!entry.isExpandable) return
        expanded = if (entry.id in expanded) expanded - entry.id else expanded + entry.id
    }

    /**
     * В буфер уходит ровно то, что на экране: с тем же фильтром и в том же
     * порядке. Журнал забирают, чтобы приложить к описанию бага, и объяснять
     * потом расхождение с увиденным незачем.
     */
    fun copyAll() {
        clipboard.copy(visible.joinToString(separator = "\n") { "${time(it)}  ${it.name}  ${summary(it)}" })
        say(R.string.analytics_copied)
    }

    /**
     * Очистка — это не уборка, а способ смотреть: журнал чистят перед
     * проверяемым флоу, чтобы в списке остался только он.
     */
    fun clear() {
        log.clear()
        expanded = emptySet()
        scopeFilter = null
        say(R.string.analytics_log_cleared)
    }

    fun goBack() {
        router.pop()
    }

    /** Свежие записи сверху: смотрят обычно то, что нажали минуту назад. */
    private val visible: List<AnalyticsRecord>
        get() = log.records.reversed().filter { scopeFilter == null || scopeOf(it) == scopeFilter }

    private fun entry(record: AnalyticsRecord): DebugAnalyticsEntry = DebugAnalyticsEntry(
        id = record.id,
        name = record.name,
        time = time(record),
        summary = summary(record),
        parameters = parameters(record),
    )

    private fun parameters(record: AnalyticsRecord): List<DebugAnalyticsParameter> {
        val content = record.content as? AnalyticsRecord.Content.Event ?: return emptyList()

        // По алфавиту: одинаковые события должны выглядеть одинаково, иначе
        // их не сравнить глазами.
        return content.parameters.entries
            .sortedBy { it.key }
            .map { DebugAnalyticsParameter(name = it.key, value = it.value.text) }
    }

    private fun summary(record: AnalyticsRecord): String = when (val content = record.content) {
        is AnalyticsRecord.Content.Event ->
            parameters(record).joinToString(separator = " · ") { "${it.name}=${it.value}" }

        is AnalyticsRecord.Content.Property ->
            resources.getString(
                R.string.analytics_property,
                content.value ?: resources.getString(R.string.analytics_value_cleared),
            )
    }

    /**
     * Префикс модуля — первое слово имени: собирает имя `FeatureAnalytics`,
     * и отдельным полем префикс в событие не приезжает.
     *
     * Свойства собраны в свою группу: их имена префикса не носят, и деление
     * по первому слову дало бы у «has_saved_result» группу «has».
     */
    private fun scopeOf(record: AnalyticsRecord): String = if (record.content is AnalyticsRecord.Content.Event) {
        record.name.substringBefore('_')
    } else {
        resources.getString(R.string.analytics_properties)
    }

    /**
     * Время с секундами и без даты: журнал не переживает перезапуск,
     * так что всё в нём случилось только что.
     */
    private fun time(record: AnalyticsRecord): String = CLOCK.format(record.date)

    private fun say(text: Int) {
        toastJob?.cancel()
        toast = resources.getString(text)
        toastJob = scope.launch {
            delay(TOAST_MILLIS)
            toast = null
        }
    }

    private companion object {
        val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
        const val TOAST_MILLIS = 2600L
    }
}
