package com.slimly.app

import android.content.res.Resources
import com.slimly.feature.audio.api.AudioRoute
import com.slimly.feature.compress.api.CompressRoute
import com.slimly.feature.convert.api.ConvertKind
import com.slimly.feature.convert.api.ConvertRoute
import com.slimly.feature.home.api.HomeTool
import com.slimly.feature.scan.api.ScanRoute
import com.slimly.navigation.Route
import com.slimly.navigation.RoutePresentation
import com.slimly.shortcuts.ToolCatalog
import com.slimly.shortcuts.ToolShortcut

/**
 * Состав хаба инструментов.
 *
 * Подписи и порядок берутся из общего каталога — того же, что у ярлыков иконки
 * и виджета: инструмент должен называться одинаково всюду, откуда его
 * открывают. А вот маршрут добавляется здесь: каталог живёт в `:shortcuts`
 * и о фичах не знает вовсе.
 *
 * Конвертация — две карточки на одну фичу: фото и видео идут одними и теми же
 * экранами, но приходят за ними по разным поводам, и общая карточка
 * «Конвертация» заставляла бы выбирать вид файла дважды.
 */
internal fun homeTools(resources: Resources): List<HomeTool> = ToolCatalog.AVAILABLE.map { tool ->
    HomeTool(
        id = tool.id.code,
        title = resources.getString(tool.title),
        subtitle = resources.getString(tool.subtitle),
        route = route(tool.id),
        presentation = presentation(tool.id),
    )
}

/**
 * Маршрут инструмента. `when` без ветки по умолчанию намеренно: новый
 * инструмент в каталоге сразу валит сборку здесь — там, где его забыли
 * открыть.
 */
private fun route(id: ToolShortcut.Id): Route = when (id) {
    ToolShortcut.Id.COMPRESS -> CompressRoute.Picker
    ToolShortcut.Id.CONVERT_PHOTO -> ConvertRoute.Picker(ConvertKind.PHOTO)
    ToolShortcut.Id.CONVERT_VIDEO -> ConvertRoute.Picker(ConvertKind.VIDEO)
    ToolShortcut.Id.SCAN -> ScanRoute.Intro
    ToolShortcut.Id.AUDIO -> AudioRoute.Picker
}

/**
 * Как открывается инструмент. У скана перед первым шагом стоит выбор
 * источника — камера или галерея, — и ради одного касания уводить с хаба
 * незачем: он приезжает шторкой. Остальные начинаются сразу с выбора файлов.
 */
private fun presentation(id: ToolShortcut.Id): RoutePresentation = when (id) {
    ToolShortcut.Id.SCAN -> RoutePresentation.SHEET

    ToolShortcut.Id.COMPRESS,
    ToolShortcut.Id.CONVERT_PHOTO,
    ToolShortcut.Id.CONVERT_VIDEO,
    ToolShortcut.Id.AUDIO,
    -> RoutePresentation.PUSH
}
