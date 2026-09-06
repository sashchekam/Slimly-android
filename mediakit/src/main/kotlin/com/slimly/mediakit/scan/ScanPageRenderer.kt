package com.slimly.mediakit.scan

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import com.slimly.core.model.DocumentQuad
import com.slimly.core.model.ScanMode
import java.io.File

/** Снимок страницы → страница скана, готовая лечь в PDF. */
internal fun interface ScanPageRendering {
    /**
     * @param quad границы листа, выбранные до обработки. `null` — их никто
     *   не задавал, и кадр берётся целиком.
     */
    fun page(source: File, mode: ScanMode, quad: DocumentQuad?): ScannedPage
}

/**
 * Сборка: сам ничего не считает, а раздаёт работу трём воркерам — геометрии
 * страницы, её чистке и кодировщику.
 */
internal class ScanPageRenderer(
    private val frames: PageFraming = PageFramer(),
    private val cleaner: PageCleaning = PageCleaner(),
    private val pages: ScanPageEncoding = ScanPageEncoder(),
) : ScanPageRendering {
    /**
     * Порядок шагов не переставляется.
     *
     * Геометрию правим по полному кадру — у выпрямления тем больше материала,
     * чем больше пикселей. Размер приводим там же, до чистки: так локальные
     * окна чистки попадают по буквам той страницы, что уедет в PDF, а не
     * по размеру исходного снимка.
     */
    override fun page(source: File, mode: ScanMode, quad: DocumentQuad?): ScannedPage {
        val frame = decode(source)
        val page = frames.page(frame, quad, mode.resolution)
        if (page !== frame) frame.recycle()

        val clean = cleaner.cleaned(page, mode)
        if (clean !== page) page.recycle()

        return pages.page(clean, mode).also { clean.recycle() }
    }

    /**
     * Разбор через `ImageDecoder`: он сам разворачивает кадр по EXIF —
     * снимок с телефона почти всегда лежит в файле боком, а лист ищется
     * в том кадре, что видит человек.
     */
    private fun decode(source: File): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(source)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }
}
