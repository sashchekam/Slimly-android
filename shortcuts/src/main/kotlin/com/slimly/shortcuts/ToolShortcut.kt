package com.slimly.shortcuts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.slimly.core.deeplink.DeeplinkUrl

/**
 * Инструмент, каким его видят точки входа снаружи приложения: виджет, ярлыки
 * иконки и хаб на главной.
 *
 * Маршрута здесь нет и быть не может: виджет живёт в чужом процессе, и
 * открывать ему нечего. Вместо маршрута — адрес `slimly://`, который на входе
 * разберёт та же таблица ссылок, что и обычную ссылку из браузера.
 *
 * Подписи лежат ключами, а не строками: каталог собирается один раз, а язык
 * приложения меняется системной настройкой — сохранённый текст остался бы
 * на прежнем языке.
 */
data class ToolShortcut(
    val id: Id,
    @get:StringRes val title: Int,
    /** Имя для тесных мест — ряда иконок в виджете, где полное не помещается. */
    @get:StringRes val short: Int,
    @get:StringRes val subtitle: Int,
    @get:DrawableRes val icon: Int,
    /** Хвост адреса после схемы: «compress», «convert/photo». */
    val path: String,
) {
    /**
     * Идентификатор — перечисление, а не строка: по нему `:app` раздаёт
     * маршруты, и новый инструмент в каталоге сразу валит сборку там,
     * где его забыли открыть.
     */
    enum class Id(val code: String) {
        COMPRESS("compress"),
        CONVERT_PHOTO("convert-photo"),
        CONVERT_VIDEO("convert-video"),
        SCAN("scan"),
        AUDIO("audio"),
    }

    /** Адрес собирается из констант каталога, поэтому разбор его не подводит. */
    val url: String
        get() = "${DeeplinkUrl.SCHEME}://$path"
}

/**
 * Каталог инструментов — один список на все точки входа: хаб на главной,
 * виджет и ярлыки иконки. Порядок здесь и есть порядок карточек на главной.
 *
 * Адреса совпадают с таблицей ссылок в `:app`: инструмент открывается ссылкой,
 * а не отдельной веткой навигации на каждую точку входа.
 */
object ToolCatalog {
    val COMPRESS = ToolShortcut(
        id = ToolShortcut.Id.COMPRESS,
        title = R.string.tool_compress_title,
        short = R.string.tool_compress_short,
        subtitle = R.string.tool_compress_subtitle,
        icon = R.drawable.ic_tool_compress,
        path = "compress",
    )

    val CONVERT_PHOTO = ToolShortcut(
        id = ToolShortcut.Id.CONVERT_PHOTO,
        title = R.string.tool_convert_photo_title,
        short = R.string.tool_convert_photo_short,
        subtitle = R.string.tool_convert_photo_subtitle,
        icon = R.drawable.ic_tool_convert_photo,
        path = "convert/photo",
    )

    val CONVERT_VIDEO = ToolShortcut(
        id = ToolShortcut.Id.CONVERT_VIDEO,
        title = R.string.tool_convert_video_title,
        short = R.string.tool_convert_video_short,
        subtitle = R.string.tool_convert_video_subtitle,
        icon = R.drawable.ic_tool_convert_video,
        path = "convert/video",
    )

    val SCAN = ToolShortcut(
        id = ToolShortcut.Id.SCAN,
        title = R.string.tool_scan_title,
        short = R.string.tool_scan_short,
        subtitle = R.string.tool_scan_subtitle,
        icon = R.drawable.ic_tool_scan,
        path = "scan",
    )

    val AUDIO = ToolShortcut(
        id = ToolShortcut.Id.AUDIO,
        title = R.string.tool_audio_title,
        short = R.string.tool_audio_short,
        subtitle = R.string.tool_audio_subtitle,
        icon = R.drawable.ic_tool_audio,
        path = "audio",
    )

    /**
     * Список собран руками, а не из `entries`: порядок инструментов — решение
     * макета, и алфавит идентификаторов ему не указ.
     */
    val ALL = listOf(COMPRESS, CONVERT_PHOTO, CONVERT_VIDEO, SCAN, AUDIO)

    /**
     * Инструменты, у которых уже есть экраны.
     *
     * Пока их было меньше, чем в каталоге, этот список отличался от [ALL]:
     * обещать кнопкой то, чего в сборке нет, нельзя. Сейчас собраны все
     * пять, но список остаётся отдельным — следующий инструмент появится
     * в каталоге раньше своих экранов, как появлялись эти.
     */
    val AVAILABLE = ALL

    fun named(code: String): ToolShortcut? = ALL.firstOrNull { it.id.code == code }
}
