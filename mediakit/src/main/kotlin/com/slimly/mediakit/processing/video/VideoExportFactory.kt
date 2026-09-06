package com.slimly.mediakit.processing.video

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.slimly.core.model.VideoCodec
import java.io.File
import androidx.media3.common.MediaItem as Media3Item

/**
 * Сборка задания для Transformer: чем кодировать и что подавать на вход.
 */
internal interface VideoExportMaking {
    fun transformer(codec: VideoCodec, bitrate: Int): Transformer

    fun item(source: File, frame: VideoFrameSize): EditedMediaItem
}

/**
 * Transformer сам выбирает кодировщик, распаковщик и упаковщик — от нас ему
 * нужны только кодек, битрейт и размер кадра.
 *
 * Подмену кодека оставляем включённой (она в `DefaultEncoderFactory`
 * по умолчанию): HEVC пишут не все устройства, и упасть на середине очереди
 * хуже, чем отдать тот же ролик в H.264.
 */
@OptIn(UnstableApi::class)
internal class VideoExportFactory(private val context: Context) : VideoExportMaking {
    override fun transformer(codec: VideoCodec, bitrate: Int): Transformer = Transformer.Builder(context)
        .setVideoMimeType(mimeType(codec))
        .setEncoderFactory(
            DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(bitrate).build())
                .build(),
        )
        .build()

    /**
     * Размер кадра задан всегда, даже когда он равен исходному.
     *
     * Это не лишняя работа, а условие: без единого эффекта Transformer
     * перекладывает дорожку в новый контейнер как есть — и заданный битрейт
     * остаётся невостребованным, а «сжатие» не сжимает.
     */
    override fun item(source: File, frame: VideoFrameSize): EditedMediaItem =
        EditedMediaItem.Builder(Media3Item.fromUri(Uri.fromFile(source)))
            .setEffects(
                Effects(
                    emptyList<AudioProcessor>(),
                    listOf<Effect>(
                        Presentation.createForWidthAndHeight(
                            frame.width,
                            frame.height,
                            Presentation.LAYOUT_SCALE_TO_FIT,
                        ),
                    ),
                ),
            )
            .build()

    private fun mimeType(codec: VideoCodec): String = when (codec) {
        VideoCodec.H264 -> MimeTypes.VIDEO_H264
        VideoCodec.HEVC -> MimeTypes.VIDEO_H265
    }
}
