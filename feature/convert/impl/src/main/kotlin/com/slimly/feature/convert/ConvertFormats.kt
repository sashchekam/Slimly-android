package com.slimly.feature.convert

import com.slimly.core.model.ImageFormat
import com.slimly.core.model.MediaFormat
import com.slimly.core.model.MediaItem
import com.slimly.core.model.ProcessingTarget
import com.slimly.core.model.VideoCodec
import com.slimly.core.model.VideoContainer

/**
 * Во что можно превратить файл — и насколько от этого изменится вес.
 *
 * Коэффициенты — оценка, а не обещание: у конвертации результат может
 * и потяжелеть, а у PNG он тяжелеет всегда. Писать при этом «−0%» было бы
 * враньём, поэтому доля честно больше единицы.
 *
 * HEIC в списке нет, в отличие от iOS: `Bitmap.compress` его не пишет,
 * а единственный системный путь к нему — связка MediaCodec и MediaMuxer,
 * и работает она не на всех устройствах. Его место занимает WebP: он и легче
 * JPEG, и пишется везде.
 */
internal val PHOTO_FORMATS = listOf(
    MediaFormat(target = ProcessingTarget.Image(ImageFormat.JPEG), ratio = 0.95),
    MediaFormat(target = ProcessingTarget.Image(ImageFormat.WEBP), ratio = 0.7),
    MediaFormat(target = ProcessingTarget.Image(ImageFormat.PNG), ratio = 2.4),
)

/**
 * У видео выбирают кодек, а не контейнер: MP4 у системного упаковщика
 * единственный, и предлагать «MP4 или MP4» было бы издевательством.
 */
internal val VIDEO_FORMATS = listOf(
    MediaFormat(
        target = ProcessingTarget.Video(container = VideoContainer.MP4, codec = VideoCodec.H264),
        ratio = 1.05,
    ),
    MediaFormat(
        target = ProcessingTarget.Video(container = VideoContainer.MP4, codec = VideoCodec.HEVC),
        ratio = 0.6,
    ),
)

/**
 * Формат исходников — левая половина пары «из → в».
 *
 * Берём по факту выбора: пока формат у всех файлов один, показываем его.
 * Разошлись — общего формата у пачки нет, и вместо выдуманного ставим слово,
 * которое верно всегда.
 */
internal fun sourceFormat(names: List<String>, fallback: String): String =
    names.filter { it.isNotEmpty() }.toSet().singleOrNull() ?: fallback

/**
 * Формат снимка по его файлу.
 *
 * Имя здесь то же, каким формат подписан в списке целей: по нему шаг исходный
 * формат из списка и убирает. Расширение как есть не годится — у файла оно
 * `jpg`, а формат зовётся JPEG, и «JPG → JPEG» выглядело бы как настоящий
 * выбор.
 */
internal fun photoFormat(item: MediaItem): String = when (item.fileExtension) {
    "jpg", "jpeg" -> ImageFormat.JPEG.displayName
    "heic", "heif" -> ImageFormat.HEIC.displayName
    "webp" -> ImageFormat.WEBP.displayName
    "png" -> ImageFormat.PNG.displayName
    else -> item.fileExtension.uppercase()
}
