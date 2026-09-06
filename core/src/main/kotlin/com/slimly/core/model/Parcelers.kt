package com.slimly.core.model

import android.os.Parcel
import kotlinx.parcelize.Parceler
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Как время уезжает в `Parcel`.
 *
 * Маршрут носит с собой выбранные файлы и итог обработки, а маршрут переживает
 * смерть процесса — значит, всё это должно уметь записываться. Своих правил
 * для `Duration` и `Instant` у `@Parcelize` нет: обе величины кладём числом.
 *
 * Отрицательное число значит «значения нет»: отрицательных длительностей
 * и дат до 1970 года у файлов медиатеки не бывает, а отдельный флаг занял бы
 * лишний байт на каждое поле.
 */
internal object DurationParceler : Parceler<Duration?> {
    override fun create(parcel: Parcel): Duration? = parcel.readLong().takeIf { it >= 0 }?.milliseconds

    override fun Duration?.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(this?.inWholeMilliseconds ?: ABSENT)
    }
}

internal object InstantParceler : Parceler<Instant?> {
    override fun create(parcel: Parcel): Instant? = parcel.readLong().takeIf { it >= 0 }?.let(Instant::ofEpochMilli)

    override fun Instant?.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(this?.toEpochMilli() ?: ABSENT)
    }
}

private const val ABSENT = -1L
