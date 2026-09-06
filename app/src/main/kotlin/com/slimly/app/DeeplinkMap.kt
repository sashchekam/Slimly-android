package com.slimly.app

import com.slimly.feature.audio.api.AudioRoute
import com.slimly.feature.compress.api.CompressRoute
import com.slimly.feature.convert.api.ConvertKind
import com.slimly.feature.convert.api.ConvertRoute
import com.slimly.feature.home.api.HomeRoute
import com.slimly.feature.profile.api.ProfileRoute
import com.slimly.feature.scan.api.ScanRoute
import com.slimly.navigation.DeeplinkDestination
import com.slimly.navigation.DeeplinkMapper

/**
 * Таблица «адрес → маршруты». Живёт в `:app`: только здесь видны маршруты
 * всех фич.
 *
 * Первым идёт главный экран: ссылка открывает инструмент, но возврат должен
 * вести на хаб, а не в пустоту. Внутренние шаги флоу ссылками не открываются —
 * они требуют конкретных файлов медиатеки, а такое в адрес не положишь.
 *
 * В таблице все пять инструментов: последним приехал скан.
 */
internal fun deeplinkMappers(): List<DeeplinkMapper> = listOf(home, compress, convert, audio, scan, profile)

/** `slimly://home` */
private val home = DeeplinkMapper { deeplink ->
    if (deeplink.host == "home" && deeplink.pathSegments.isEmpty()) {
        DeeplinkDestination(HomeRoute.Home)
    } else {
        null
    }
}

/** `slimly://compress` */
private val compress = DeeplinkMapper { deeplink ->
    if (deeplink.host == "compress" && deeplink.pathSegments.isEmpty()) {
        DeeplinkDestination(HomeRoute.Home, CompressRoute.Picker)
    } else {
        null
    }
}

/**
 * `slimly://convert/photo`, `slimly://convert/video` — вид файла частью
 * адреса, а не отдельным хостом: инструмент один, а вход в него два.
 */
private val convert = DeeplinkMapper { deeplink ->
    if (deeplink.host != "convert") return@DeeplinkMapper null

    val kind = when (deeplink.pathSegments) {
        listOf("photo") -> ConvertKind.PHOTO
        listOf("video") -> ConvertKind.VIDEO
        else -> null
    }
    kind?.let { DeeplinkDestination(HomeRoute.Home, ConvertRoute.Picker(it)) }
}

/** `slimly://audio` */
private val audio = DeeplinkMapper { deeplink ->
    if (deeplink.host == "audio" && deeplink.pathSegments.isEmpty()) {
        DeeplinkDestination(HomeRoute.Home, AudioRoute.Picker)
    } else {
        null
    }
}

/**
 * `slimly://scan` — шторка выбора источника поверх хаба: ссылка не должна
 * показывать инструмент иначе, чем кнопка на главной.
 */
private val scan = DeeplinkMapper { deeplink ->
    if (deeplink.host == "scan" && deeplink.pathSegments.isEmpty()) {
        DeeplinkDestination(routes = listOf(HomeRoute.Home), sheet = ScanRoute.Intro)
    } else {
        null
    }
}

/**
 * `slimly://profile`, `slimly://profile/stats` — подробная статистика
 * открывается стеком поверх профиля: возврат из неё должен вести в профиль,
 * а не сразу на хаб.
 */
private val profile = DeeplinkMapper { deeplink ->
    if (deeplink.host != "profile") return@DeeplinkMapper null

    when (deeplink.pathSegments) {
        emptyList<String>() -> DeeplinkDestination(HomeRoute.Home, ProfileRoute.Profile)
        listOf("stats") -> DeeplinkDestination(HomeRoute.Home, ProfileRoute.Profile, ProfileRoute.Statistics)
        else -> null
    }
}
