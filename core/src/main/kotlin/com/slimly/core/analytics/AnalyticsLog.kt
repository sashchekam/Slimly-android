package com.slimly.core.analytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.Instant
import java.util.UUID

/** Строка журнала: что ушло в аналитику и когда. */
data class AnalyticsRecord(
    val name: String,
    val content: Content,
    /**
     * Время ставится в момент создания записи, а не в момент попадания
     * в журнал: до журнала запись добирается прыжком на главный поток,
     * и показывать нужно момент отправки.
     */
    val date: Instant = Instant.now(),
    val id: UUID = UUID.randomUUID(),
) {
    /**
     * События и свойства лежат в одном списке: свойство уедет со всеми
     * следующими событиями, и смотреть его отдельно — значит не заметить,
     * что оно встало позже, чем нужно.
     */
    sealed interface Content {
        data class Event(val parameters: Map<String, AnalyticsValue>) : Content

        /** `null` — свойство сняли. */
        data class Property(val value: String?) : Content
    }
}

/**
 * Журнал отправленной аналитики — зеркало для отладочного меню.
 *
 * Живёт в памяти и только до перезапуска: журнал нужен, чтобы посмотреть
 * только что пройденный флоу, а не собрать историю установки. Отсюда и предел
 * на количество: длинный список всё равно никто не пролистает, а память
 * он держит.
 *
 * Живёт на главном потоке — там же, где экран, который его показывает.
 */
class AnalyticsLog(
    /**
     * Сколько последних записей остаётся. Число видно на экране журнала,
     * поэтому оно и не спрятано в приватную константу.
     */
    val capacity: Int = DEFAULT_CAPACITY,
) {
    var records: List<AnalyticsRecord> by mutableStateOf(emptyList())
        private set

    fun append(record: AnalyticsRecord) {
        // Записи приходят с разных потоков и до главного добираются не в том
        // порядке, в каком отправлялись: место ищем по времени отправки,
        // иначе соседние события меняются в списке местами.
        val index = records.indexOfLast { it.date <= record.date } + 1
        records = (records.subList(0, index) + record + records.subList(index, records.size))
            .takeLast(capacity)
    }

    fun clear() {
        records = emptyList()
    }

    private companion object {
        const val DEFAULT_CAPACITY = 300
    }
}
