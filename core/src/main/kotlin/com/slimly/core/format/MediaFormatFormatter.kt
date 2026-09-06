package com.slimly.core.format

import android.content.res.Resources
import com.slimly.core.R
import com.slimly.core.model.AudioFormat
import com.slimly.core.model.MediaFormat
import com.slimly.core.model.ProcessingTarget

interface MediaFormatFormatting {
    /** «JPEG», «MP4 · H.264», «M4A · 128 кбит/с», «PDF · ч/б». */
    fun displayName(target: ProcessingTarget): String

    fun displayName(format: MediaFormat): String
}

/**
 * Подписи форматов.
 *
 * Живут отдельно от самих форматов: короткое имя («JPEG») уезжает в журнал
 * работ и обязано читаться одинаково на любом языке, а подпись на экране
 * переводится — и складывать оба смысла в один тип не стоит.
 */
class MediaFormatFormatter(private val resources: Resources) : MediaFormatFormatting {
    override fun displayName(target: ProcessingTarget): String = when (target) {
        is ProcessingTarget.Image -> target.format.displayName
        is ProcessingTarget.Video -> "${target.container.displayName} · ${target.codec.displayName}"
        is ProcessingTarget.Audio -> audioName(target.format)
        is ProcessingTarget.Document -> "PDF · ${resources.getString(target.mode.displayNameRes)}"
    }

    override fun displayName(format: MediaFormat): String = displayName(format.target)

    private fun audioName(format: AudioFormat): String = when (format) {
        is AudioFormat.Aac -> resources.getString(R.string.audio_format_aac, format.kilobits)
        AudioFormat.LinearPcm -> resources.getString(R.string.audio_format_wav)
    }
}
