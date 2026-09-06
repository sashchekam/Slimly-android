package com.slimly.mediakit.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Сборщик PDF пишет формат руками, и проверить его можно так же, как читает
 * просмотрщик: по байтам готового файла.
 */
class PdfDocumentWriterTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val writer = PdfDocumentWriter()

    @Test
    fun `документ начинается заголовком и кончается меткой конца`() {
        val text = write(listOf(page()))

        assertTrue(text.startsWith("%PDF-1.4"))
        assertTrue(text.trimEnd().endsWith("%%EOF"))
    }

    @Test
    fun `дерево страниц знает их число`() {
        val text = write(List(3) { page() })

        assertTrue("Нет счётчика страниц", text.contains("/Type /Pages /Count 3"))
        assertEquals(3, Regex("/Type /Page[^s]").findAll(text).count())
    }

    @Test
    fun `двухцветная страница уходит однобитным растром`() {
        val text = write(listOf(page(width = 16, height = 4)))

        assertTrue(text.contains("/BitsPerComponent 1"))
        assertTrue(text.contains("/ColorSpace /DeviceGray"))
        assertTrue(text.contains("/FlateDecode"))
        assertTrue(text.contains("/Width 16"))
        assertTrue(text.contains("/Height 4"))
    }

    @Test
    fun `таблица ссылок указывает на настоящие объекты`() {
        val bytes = writtenBytes(listOf(page()))
        val text = bytes.toString(Charsets.ISO_8859_1)

        val startxref = text.substringAfterLast("startxref").trim().substringBefore("\n").trim().toInt()
        assertTrue("Смещение $startxref вне файла", startxref in 0 until bytes.size)
        assertTrue(text.substring(startxref).startsWith("xref"))
    }

    @Test
    fun `заказ без единой страницы документом не становится`() {
        val destination = folder.newFile("empty.pdf")

        val thrown = runCatching { writer.write(listOf(page(width = 0)), destination, "Скан") }.exceptionOrNull()

        assertTrue("Ожидали отказ сборки, получили $thrown", thrown != null)
    }

    private fun page(width: Int = 8, height: Int = 8): ScannedPage.Bilevel {
        val rowBytes = (width + 7) / 8
        return ScannedPage.Bilevel(
            width = width,
            height = height,
            rowBytes = rowBytes,
            bits = ByteArray(rowBytes * height) { 0xFF.toByte() },
        )
    }

    private fun write(pages: List<ScannedPage>): String = writtenBytes(pages).toString(Charsets.ISO_8859_1)

    private fun writtenBytes(pages: List<ScannedPage>): ByteArray {
        val destination = folder.newFile("scan-${pages.size}-${System.nanoTime()}.pdf")
        writer.write(pages, destination, "Скан")
        return destination.readBytes()
    }
}
