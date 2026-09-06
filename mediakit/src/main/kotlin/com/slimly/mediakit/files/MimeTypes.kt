package com.slimly.mediakit.files

/**
 * MIME-тип по имени файла. `null` — расширение незнакомое.
 *
 * Своя таблица, а не системная `MimeTypeMap`: на разных прошивках она знает
 * разное, а список расширений у приложения закрытый — их ровно столько,
 * сколько форматов оно умеет записать.
 */
internal fun mimeType(name: String): String? =
    MIME_TYPES[name.substringAfterLast(delimiter = '.', missingDelimiterValue = "").lowercase()]

/** Чем подписать файл, тип которого не угадан: «просто байты». */
internal const val OCTET_STREAM = "application/octet-stream"

private val MIME_TYPES = mapOf(
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "png" to "image/png",
    "webp" to "image/webp",
    "heic" to "image/heic",
    "mp4" to "video/mp4",
    // AAC в контейнере M4A — это MPEG-4, а не «audio/aac»: тип по контейнеру,
    // иначе плееры отказываются открывать дорожку.
    "m4a" to "audio/mp4",
    "wav" to "audio/x-wav",
    "pdf" to "application/pdf",
)
