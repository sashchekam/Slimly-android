package com.slimly.mediakit.processing.video

import android.content.Context
import com.slimly.core.model.VideoCodec
import java.io.File

internal data class VideoTranscodeSettings(
    /**
     * Кодек результата. `null` — оставить тот, которым снят исходник:
     * так работает сжатие, которое тип файла менять не должно.
     */
    val codec: VideoCodec?,
    /**
     * Ограничение короткой стороны: 1080 — это «1080p» и для вертикали,
     * и для горизонтали. `null` — размер кадра не трогаем.
     */
    val shortSideLimit: Int?,
    /** Доля исходного битрейта. */
    val bitrateFactor: Double,
)

internal interface VideoTranscoding {
    suspend fun transcode(
        source: File,
        destination: File,
        settings: VideoTranscodeSettings,
        progress: (Double) -> Unit,
    )
}

/**
 * Перекодирование видео.
 *
 * Здесь только сборка: описание исходника, битрейт, размер кадра и запуск —
 * каждое своим воркером. Контейнер результата всегда MP4: другого системный
 * упаковщик не пишет.
 */
internal class VideoTranscoder(
    context: Context,
    private val sources: VideoSourceReading = VideoSourceReader(),
    private val bitrates: VideoBitrateEstimating = VideoBitrateEstimator(),
    private val frames: VideoFrameSizing = VideoFrameSizer(),
    private val exports: VideoExportMaking = VideoExportFactory(context),
    private val runner: VideoExportRunning = VideoExportRunner(),
) : VideoTranscoding {
    override suspend fun transcode(
        source: File,
        destination: File,
        settings: VideoTranscodeSettings,
        progress: (Double) -> Unit,
    ) {
        val info = sources.read(source)

        val bitrate = bitrates.bitrate(
            source = source,
            sourceBitrate = info.bitrate,
            duration = info.duration,
            factor = settings.bitrateFactor,
        )
        val frame = frames.size(
            width = info.width,
            height = info.height,
            shortSideLimit = settings.shortSideLimit,
        )
        // Кодек исходника незнаком — берём H.264: его пишет любое устройство,
        // и открывается он везде.
        val codec = settings.codec ?: info.codec ?: VideoCodec.H264

        runner.run(
            transformer = exports.transformer(codec, bitrate),
            item = exports.item(source, frame),
            destination = destination,
            progress = progress,
        )
    }
}
