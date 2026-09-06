package com.slimly.core.format

import android.content.res.Resources
import com.slimly.core.R
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.model.ProcessingResult
import kotlin.math.roundToInt

interface MediaItemFormatting {
    /** Подпись на плитке галереи. */
    fun kindLabel(item: MediaItem): String

    /** Строка под плиткой: у видео — длительность и вес, у фото — только вес. */
    fun meta(item: MediaItem, showsSize: Boolean = true): String

    /** «840 МБ → 302 МБ». */
    fun sizeTransition(from: Long, to: Long): String

    /** «−64%». */
    fun reduction(result: ProcessingResult): String

    /** Доля в процентах со знаком: «−58%», «+140%». */
    fun percent(fraction: Double): String
}

class MediaItemFormatter(
    private val resources: Resources,
    private val sizes: FileSizeFormatting = FileSizeFormatter(resources),
    private val durations: DurationFormatting = DurationFormatter(resources),
) : MediaItemFormatting {
    override fun kindLabel(item: MediaItem): String = when (item.kind) {
        MediaKind.VIDEO -> resources.getString(R.string.badge_video)
        MediaKind.PHOTO -> if (item.isHeic) "HEIC" else resources.getString(R.string.badge_photo)
    }

    /**
     * Вес нужен не везде: скану безразлично, сколько весит снимок страницы, —
     * там это число ни на что не влияет и только шумит.
     */
    override fun meta(item: MediaItem, showsSize: Boolean): String {
        val size = if (showsSize) sizes.string(item.byteCount) else ""
        val duration = item.duration ?: return size
        val time = durations.string(duration)
        return if (size.isEmpty()) time else "$time · $size"
    }

    override fun sizeTransition(from: Long, to: Long): String = "${sizes.string(from)} → ${sizes.string(to)}"

    override fun reduction(result: ProcessingResult): String = percent(result.reduction)

    override fun percent(fraction: Double): String {
        val value = (fraction * PERCENT).roundToInt()
        return if (value < 0) "+${-value}%" else "−$value%"
    }

    private companion object {
        const val PERCENT = 100
    }
}
