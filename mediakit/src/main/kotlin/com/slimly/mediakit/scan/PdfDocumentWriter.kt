package com.slimly.mediakit.scan

import com.slimly.mediakit.processing.ProcessingError
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Сборка готовых страниц в один PDF. */
internal fun interface DocumentWriting {
    /** @param pages страницы в порядке следования в документе. */
    fun write(pages: List<ScannedPage>, destination: File, title: String)
}

/**
 * PDF пишем сами, а не через `PdfDocument` системы.
 *
 * Системный сборщик умеет только рисовать на холсте: любую картинку он
 * растрирует и кладёт в страницу разжатой. Скан от этого тяжелеет
 * в десятки раз — а весь смысл двух режимов страницы как раз в весе.
 * Здесь же готовый JPEG уходит в документ теми же байтами, потоком
 * `DCTDecode`, а двухцветная страница — однобитным растром под Flate.
 *
 * Формат при этом самый простой из возможных: линейная таблица объектов,
 * по одной картинке на страницу, без шрифтов и прозрачности. Ровно то же,
 * что отдаёт CoreGraphics на iOS.
 */
internal class PdfDocumentWriter : DocumentWriting {
    override fun write(pages: List<ScannedPage>, destination: File, title: String) {
        val drawable = pages.filter { it.width > 0 && it.height > 0 }
        if (drawable.isEmpty()) throw ProcessingError.PdfAssemblyFailed()

        destination.outputStream().buffered().use { output ->
            Writer(output).document(drawable, title)
        }
    }
}

/**
 * Один проход записи. Считает смещения объектов на лету: таблица ссылок
 * в конце файла обязана указывать на байты, а узнать их можно только
 * записывая.
 */
private class Writer(private val output: OutputStream) {
    private val offsets = mutableListOf<Int>()
    private var position = 0

    fun document(pages: List<ScannedPage>, title: String) {
        // Объекты: 1 — каталог, 2 — дерево страниц, 3 — сведения о документе,
        // дальше по три на страницу (страница, её содержимое, картинка).
        val first = FIRST_PAGE_OBJECT
        val pageIds = pages.indices.map { first + it * OBJECTS_PER_PAGE }

        write("%PDF-1.4\n")
        // Четыре байта старше 127: подсказка тому, кто откроет файл, что
        // это не текст. Так велит сам формат.
        writeBytes(BINARY_MARKER)

        obj(CATALOG) { write("<< /Type /Catalog /Pages $PAGES 0 R >>") }
        obj(PAGES) {
            write("<< /Type /Pages /Count ${pages.size} /Kids [")
            pageIds.forEach { write(" $it 0 R") }
            write(" ] >>")
        }
        obj(INFO) { write("<< /Title (${escaped(title)}) /Producer (Slimly) /Creator (Slimly) >>") }

        pages.forEachIndexed { index, page -> page(page, pageIds[index]) }

        val startxref = position
        crossReferences(pages.size)
        write("trailer\n<< /Size ${offsets.size + 1} /Root $CATALOG 0 R /Info $INFO 0 R >>\n")
        write("startxref\n$startxref\n%%EOF\n")
        output.flush()
    }

    /** Страница, её содержимое и картинка — три объекта подряд. */
    private fun page(page: ScannedPage, id: Int) {
        val box = box(page)
        val frame = frame(page, box)
        val content = "q ${frame.width} 0 0 ${frame.height} ${frame.x} ${frame.y} cm /Im0 Do Q\n"

        obj(id) {
            write(
                "<< /Type /Page /Parent $PAGES 0 R " +
                    "/MediaBox [0 0 ${box.width} ${box.height}] " +
                    "/Resources << /XObject << /Im0 ${id + 2} 0 R >> >> " +
                    "/Contents ${id + 1} 0 R >>",
            )
        }

        obj(id + 1) {
            write("<< /Length ${content.length} >>\nstream\n")
            write(content)
            write("endstream")
        }

        obj(id + 2) { image(page) }
    }

    private fun image(page: ScannedPage) {
        when (page) {
            is ScannedPage.Jpeg -> {
                val space = if (page.isGray) "/DeviceGray" else "/DeviceRGB"
                write(
                    "<< /Type /XObject /Subtype /Image /Width ${page.width} /Height ${page.height} " +
                        "/ColorSpace $space /BitsPerComponent 8 /Filter /DCTDecode " +
                        "/Length ${page.data.size} >>\nstream\n",
                )
                writeBytes(page.data)
                write("\nendstream")
            }

            is ScannedPage.Bilevel -> {
                // Единица — белый, поэтому `/Decode` переворачивать не нужно:
                // в DeviceGray единица и есть белый.
                val deflated = deflated(page.bits)
                write(
                    "<< /Type /XObject /Subtype /Image /Width ${page.width} /Height ${page.height} " +
                        "/ColorSpace /DeviceGray /BitsPerComponent 1 /Filter /FlateDecode " +
                        "/Length ${deflated.size} >>\nstream\n",
                )
                writeBytes(deflated)
                write("\nendstream")
            }
        }
    }

    /**
     * Лист под страницу: её собственной формы, длинной стороной в A4.
     *
     * Вписывать всё подряд в A4 значило бы обрамлять чек полями в половину
     * листа. Ровно A4 оставляем для страницы, которая им и является: такая
     * печатается один в один, без пересчёта.
     */
    private fun box(page: ScannedPage): Box {
        val isLandscape = page.width > page.height
        val sheetWidth = if (isLandscape) A4_HEIGHT else A4_WIDTH
        val sheetHeight = if (isLandscape) A4_WIDTH else A4_HEIGHT
        val ratio = page.width.toFloat() / page.height

        if (abs(ratio / (sheetWidth / sheetHeight) - 1) <= A4_TOLERANCE) return Box(sheetWidth, sheetHeight)

        val long = max(sheetWidth, sheetHeight)
        return if (ratio > 1) Box(long, long / ratio) else Box(long * ratio, long)
    }

    private fun frame(page: ScannedPage, box: Box): Frame {
        val scale = min(box.width / page.width, box.height / page.height)
        val width = page.width * scale
        val height = page.height * scale
        return Frame(x = (box.width - width) / 2, y = (box.height - height) / 2, width = width, height = height)
    }

    private fun crossReferences(pageCount: Int) {
        val total = FIRST_PAGE_OBJECT + pageCount * OBJECTS_PER_PAGE
        write("xref\n0 $total\n")
        write("0000000000 65535 f \n")
        for (id in 1 until total) {
            val offset = offsets.getOrElse(id - 1) { 0 }
            write(offset.toString().padStart(XREF_DIGITS, '0') + " 00000 n \n")
        }
    }

    private fun obj(id: Int, body: () -> Unit) {
        while (offsets.size < id) offsets.add(0)
        offsets[id - 1] = position
        write("$id 0 obj\n")
        body()
        write("\nendobj\n")
    }

    private fun deflated(bytes: ByteArray): ByteArray {
        val target = ByteArrayOutputStream()
        DeflaterOutputStream(target, Deflater(Deflater.BEST_COMPRESSION)).use { it.write(bytes) }
        return target.toByteArray()
    }

    /** Скобки и обратная косая в строке PDF — служебные знаки. */
    private fun escaped(text: String): String = text
        .replace("\\", "\\\\")
        .replace("(", "\\(")
        .replace(")", "\\)")

    private fun write(text: String) = writeBytes(text.toByteArray(Charsets.ISO_8859_1))

    private fun writeBytes(bytes: ByteArray) {
        output.write(bytes)
        position += bytes.size
    }

    private data class Box(val width: Float, val height: Float)

    private data class Frame(val x: Float, val y: Float, val width: Float, val height: Float)

    private companion object {
        const val CATALOG = 1
        const val PAGES = 2
        const val INFO = 3
        const val FIRST_PAGE_OBJECT = 4
        const val OBJECTS_PER_PAGE = 3
        const val XREF_DIGITS = 10

        /** A4 в точках PDF (72 dpi) — размер, на котором документ печатается без пересчёта. */
        const val A4_WIDTH = 595f
        const val A4_HEIGHT = 842f

        /**
         * Насколько пропорции страницы могут разойтись с A4, чтобы она всё
         * ещё считалась им. Шесть процентов — это погрешность углов,
         * поставленных пальцем на экране правки.
         */
        const val A4_TOLERANCE = 0.06f

        /** «%» плюс четыре байта старше 127 и перевод строки. */
        val BINARY_MARKER = byteArrayOf(0x25, 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), 0x0A)
    }
}
