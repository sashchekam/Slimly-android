package com.slimly.mediakit.processing

import android.content.res.Resources
import androidx.annotation.StringRes
import com.slimly.core.service.MediaLibraryError
import com.slimly.mediakit.R

/**
 * Отказ по одному файлу. Текст попадает в список итога, поэтому написан
 * для пользователя, а не для лога.
 */
internal sealed class ProcessingError(
    @get:StringRes val messageRes: Int,
    val argument: String? = null,
    cause: Throwable? = null,
) : Exception(cause) {
    class UnreadableSource(cause: Throwable) : ProcessingError(R.string.failure_unreadable_source, cause = cause)

    class UnsupportedFormat(name: String) : ProcessingError(R.string.failure_unsupported_format, name)

    class WriteFailed(reason: String, cause: Throwable? = null) :
        ProcessingError(R.string.failure_write_failed, reason, cause)

    /** Сохранять нечего: записанного файла нет. */
    class OutputMissing : ProcessingError(R.string.failure_result_missing)

    class UnsupportedOperation : ProcessingError(R.string.failure_unsupported_operation)

    /** Инструменту дали не тот вид файла: у форматов картинки и видео разные. */
    class VideoTarget(name: String) : ProcessingError(R.string.failure_video_target, name)

    class PhotoTarget(name: String) : ProcessingError(R.string.failure_photo_target, name)

    /** Звук просят из фотографии: дорожки там нет и быть не может. */
    class AudioFromPhoto : ProcessingError(R.string.failure_audio_from_photo)

    /** Видео есть, а звука в нём нет — снимали без микрофона. */
    class NoAudioTrack(cause: Throwable) : ProcessingError(R.string.failure_no_audio_track, cause = cause)

    /** Система отказалась писать страницу скана. */
    class PageEncodingFailed : ProcessingError(R.string.failure_write_failed, "JPEG")

    /** Не собралось ни одной страницы — документа нет. */
    class PdfAssemblyFailed : ProcessingError(R.string.failure_pdf_assembly)
}

/**
 * Причина отказа так, как её увидят на экране итога.
 *
 * Отдельным воркером, потому что источников текста три — свои отказы,
 * отказы медиатеки и всё остальное, — а ресурсы нужны только здесь.
 */
internal fun interface FailureMessaging {
    fun message(error: Throwable): String
}

internal class FailureMessages(private val resources: Resources) : FailureMessaging {
    override fun message(error: Throwable): String = when (error) {
        is ProcessingError ->
            error.argument
                ?.let { resources.getString(error.messageRes, it) }
                ?: resources.getString(error.messageRes)

        is MediaLibraryError -> error.messageRes?.let(resources::getString) ?: error.message.orEmpty()

        else -> error.message.orEmpty()
    }
}
