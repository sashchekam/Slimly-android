package com.slimly.mediakit.processing

import androidx.exifinterface.media.ExifInterface
import java.io.File

/** Перенос метаданных оригинала на готовый файл. */
internal fun interface ExifTransferring {
    fun transfer(source: File, destination: File)
}

/**
 * `Bitmap` — это растр без метаданных: дата съёмки, геометка и модель камеры
 * при перекодировании пропадают. Дату результату возвращает запись
 * в медиатеку (`DATE_TAKEN`), а всё остальное живёт только внутри файла —
 * и заметно ровно тогда, когда файл отдают наружу.
 *
 * Поэтому метаданные переносятся отдельным шагом поверх уже записанного
 * файла, как и на iOS: правило «что оставить» меняется чаще, чем сама
 * запись.
 *
 * Молчаливый: потерянная геометка — не повод отменить сжатие целиком,
 * а форматов, которым `ExifInterface` умеет дописать метаданные, всего три
 * (JPEG, PNG, WebP) — ровно те, что мы и пишем.
 */
internal class ExifTransfer : ExifTransferring {
    override fun transfer(source: File, destination: File) {
        runCatching {
            val from = ExifInterface(source)
            val to = ExifInterface(destination)

            TAGS.forEach { tag -> from.getAttribute(tag)?.let { to.setAttribute(tag, it) } }

            // Ориентацию не переносим, а сбрасываем: `ImageDecoder` уже
            // развернул кадр по ней, и второй поворот положил бы его на бок.
            to.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            to.saveAttributes()
        }
    }

    private companion object {
        /**
         * Что переносим. Списком, а не «всё подряд»: размеры кадра после
         * уменьшения врали бы о файле, а полосы миниатюры внутри EXIF
         * весят больше, чем стоят.
         */
        val TAGS = listOf(
            // Когда снято — главное, ради чего всё и затевалось.
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_OFFSET_TIME,
            ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
            ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
            ExifInterface.TAG_SUBSEC_TIME,
            ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
            ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,

            // Где снято.
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,

            // Чем снято.
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_LENS_MAKE,
            ExifInterface.TAG_LENS_MODEL,
            ExifInterface.TAG_SOFTWARE,
            ExifInterface.TAG_ARTIST,
            ExifInterface.TAG_COPYRIGHT,
            ExifInterface.TAG_USER_COMMENT,

            // Как снято: эти поля читает любая программа разбора снимков.
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_F_NUMBER,
            // `TAG_ISO_SPEED_RATINGS` — то же поле под старым именем,
            // и брать его вторым разом незачем.
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
            ExifInterface.TAG_APERTURE_VALUE,
            ExifInterface.TAG_SHUTTER_SPEED_VALUE,
            ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
            ExifInterface.TAG_METERING_MODE,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_SCENE_CAPTURE_TYPE,
        )
    }
}
