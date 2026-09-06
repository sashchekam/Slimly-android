package com.slimly.core.format

import android.content.res.Resources

/**
 * Все форматтеры одним свёртком.
 *
 * На iOS они статические, и брать их можно откуда угодно; здесь каждому
 * нужны ресурсы, а значит и передача параметром. Свёрток нужен затем, чтобы
 * экран принимал один параметр вместо шести — сам он ничего не делает.
 */
class SlimlyFormatters(
    resources: Resources,
    val sizes: FileSizeFormatting = FileSizeFormatter(resources),
    val counts: CountFormatting = CountFormatter(resources),
    val durations: DurationFormatting = DurationFormatter(resources),
    val dates: MediaDateFormatting = MediaDateFormatter(resources),
    val items: MediaItemFormatting = MediaItemFormatter(resources),
    val formats: MediaFormatFormatting = MediaFormatFormatter(resources),
)
