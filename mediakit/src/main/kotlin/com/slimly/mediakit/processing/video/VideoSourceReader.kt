package com.slimly.mediakit.processing.video

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import com.slimly.core.model.VideoCodec
import com.slimly.mediakit.processing.ProcessingError
import java.io.File
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Что известно об исходном ролике до перекодирования.
 *
 * Размер кадра — тот, каким его видят: если файл снят с поворотом на 90°,
 * стороны здесь уже переставлены. Так же его понимает и `Presentation`
 * из Media3, поэтому считать масштаб можно прямо по этим числам.
 */
internal data class VideoSourceInfo(
    val width: Int,
    val height: Int,
    val duration: Duration,
    /** Битрейт по данным системы. Ноль — система его не сообщила. */
    val bitrate: Int,
    /** Кодек дорожки. `null` — контейнер не открылся или кодек нам незнаком. */
    val codec: VideoCodec?,
) {
    val shortSide: Int
        get() = minOf(width, height)
}

internal fun interface VideoSourceReading {
    fun read(source: File): VideoSourceInfo
}

/**
 * Описание исходника двумя системными читателями сразу.
 *
 * `MediaMetadataRetriever` знает про файл целиком — размер кадра, поворот,
 * длительность, — но не говорит, каким кодеком сжата картинка. Кодек и его
 * собственный битрейт лежат в описании дорожки, а до него добирается только
 * `MediaExtractor`. Поэтому читателя два, а работа одна.
 */
internal class VideoSourceReader : VideoSourceReading {
    override fun read(source: File): VideoSourceInfo {
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(source.path)

            val width = retriever.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotation = retriever.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val turned = rotation == QUARTER_TURN || rotation == THREE_QUARTER_TURNS
            val track = track(source)

            return VideoSourceInfo(
                width = if (turned) height else width,
                height = if (turned) width else height,
                duration = retriever.int(MediaMetadataRetriever.METADATA_KEY_DURATION).milliseconds,
                // Битрейт дорожки точнее общего: в общий входит и звук,
                // а кодировщику мы задаём только картинку.
                bitrate = track.bitrate.takeIf { it > 0 }
                    ?: retriever.int(MediaMetadataRetriever.METADATA_KEY_BITRATE),
                codec = track.codec,
            )
            // `setDataSource` отвечает одним и тем же на всё разом: и на битый
            // контейнер, и на файл, которого уже нет, — перечислить исключения
            // точнее система не даёт.
        } catch (@Suppress("TooGenericExceptionCaught") error: RuntimeException) {
            throw ProcessingError.UnreadableSource(error)
        } finally {
            retriever.release()
        }
    }

    private fun MediaMetadataRetriever.int(key: Int): Int = extractMetadata(key)?.toIntOrNull() ?: 0

    /** Кодек и битрейт видеодорожки. Пустой ответ — про дорожку ничего не известно. */
    private fun track(source: File): VideoTrack {
        val extractor = MediaExtractor()

        return try {
            extractor.setDataSource(source.path)
            (0 until extractor.trackCount)
                .map(extractor::getTrackFormat)
                .firstOrNull { it.mime.startsWith(VIDEO_MIME_PREFIX) }
                ?.let { VideoTrack(codec = codec(it.mime), bitrate = it.bitrate) }
                ?: VideoTrack()
            // Кодек — не то, ради чего стоит ронять обработку: без него
            // возьмём тот, что задан по умолчанию, а сам файл всё равно
            // откроет Transformer.
        } catch (@Suppress("SwallowedException") error: IOException) {
            VideoTrack()
        } finally {
            extractor.release()
        }
    }

    private val MediaFormat.mime: String
        get() = getString(MediaFormat.KEY_MIME).orEmpty()

    private val MediaFormat.bitrate: Int
        get() = if (containsKey(MediaFormat.KEY_BIT_RATE)) getInteger(MediaFormat.KEY_BIT_RATE) else 0

    private fun codec(mime: String): VideoCodec? = when (mime) {
        MediaFormat.MIMETYPE_VIDEO_AVC -> VideoCodec.H264

        MediaFormat.MIMETYPE_VIDEO_HEVC -> VideoCodec.HEVC

        // VP9, AV1 и прочее: читать их система умеет, а предлагать их
        // как цель мы не станем — писать нечем.
        else -> null
    }

    private data class VideoTrack(val codec: VideoCodec? = null, val bitrate: Int = 0)

    private companion object {
        const val QUARTER_TURN = 90
        const val THREE_QUARTER_TURNS = 270
        const val VIDEO_MIME_PREFIX = "video/"
    }
}
