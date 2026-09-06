package com.slimly.mediakit.processing.png

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.CRC32
import java.util.zip.Inflater

/**
 * PNG с палитрой пишется руками, и проверяется он так же, как читает
 * просмотрщик: по чанкам готовых байтов.
 */
class IndexedPngWriterTest {
    private val writer = IndexedPngWriter()

    @Test
    fun `файл начинается подписью и кончается концом`() {
        val bytes = requireNotNull(writer.bytes(bitmap(width = 4, height = 2, colors = 4)))

        assertArrayEquals(SIGNATURE, bytes.copyOf(SIGNATURE.size))
        assertEquals(listOf("IHDR", "PLTE", "IDAT", "IEND"), chunks(bytes).map { it.type })
    }

    @Test
    fun `у каждого чанка своя контрольная сумма`() {
        val bytes = requireNotNull(writer.bytes(bitmap(width = 7, height = 3, colors = 5)))

        chunks(bytes).forEach { chunk ->
            val body = chunk.type.toByteArray(Charsets.US_ASCII) + chunk.payload
            assertEquals("Сумма чанка ${chunk.type}", CRC32().apply { update(body) }.value.toInt(), chunk.crc)
        }
    }

    @Test
    fun `длина палитры выбирает глубину индекса`() {
        assertEquals(1, depth(colors = 2))
        assertEquals(2, depth(colors = 4))
        assertEquals(4, depth(colors = 16))
        assertEquals(8, depth(colors = 200))
    }

    @Test
    fun `индексы восстанавливаются из потока байт в байт`() {
        val bitmap = bitmap(width = 5, height = 3, colors = 200)
        val bytes = requireNotNull(writer.bytes(bitmap))

        val rows = inflate(chunks(bytes).first { it.type == "IDAT" }.payload)
        // Строка = байт фильтра плюс по индексу на пиксель.
        assertEquals(bitmap.height * (bitmap.width + 1), rows.size)
        for (y in 0 until bitmap.height) {
            assertEquals("Фильтр строки $y", 0, rows[y * (bitmap.width + 1)].toInt())
            for (x in 0 until bitmap.width) {
                assertEquals(bitmap.indices[y * bitmap.width + x], rows[y * (bitmap.width + 1) + 1 + x])
            }
        }
    }

    @Test
    fun `непрозрачной палитре чанк прозрачности не нужен`() {
        val bytes = requireNotNull(writer.bytes(bitmap(width = 4, height = 4, colors = 4)))

        assertTrue(chunks(bytes).none { it.type == "tRNS" })
    }

    @Test
    fun `прозрачные записи уезжают в начало палитры`() {
        val palette = listOf(
            PackedColor.of(10, 10, 10, 255),
            PackedColor.of(20, 20, 20, 255),
            PackedColor.of(30, 30, 30, 0),
        )
        val bytes = requireNotNull(
            writer.bytes(
                IndexedBitmap(width = 3, height = 1, palette = palette, indices = byteArrayOf(0, 1, 2)),
            ),
        )

        val transparency = chunks(bytes).first { it.type == "tRNS" }.payload
        // Дырка одна и стоит первой — чанк обрывается на ней.
        assertEquals(1, transparency.size)
        assertEquals(0, transparency[0].toInt())
    }

    @Test
    fun `пустой палитре писать нечего`() {
        assertNull(writer.bytes(IndexedBitmap(width = 2, height = 2, palette = emptyList(), indices = ByteArray(4))))
        assertNull(
            writer.bytes(
                IndexedBitmap(
                    width = 0,
                    height = 0,
                    palette = listOf(PackedColor.of(0, 0, 0, 255)),
                    indices = ByteArray(0),
                ),
            ),
        )
    }

    private fun depth(colors: Int): Int {
        val bytes = requireNotNull(writer.bytes(bitmap(width = 8, height = 2, colors = colors)))
        return chunks(bytes).first { it.type == "IHDR" }.payload[8].toInt()
    }

    private fun bitmap(width: Int, height: Int, colors: Int): IndexedBitmap = IndexedBitmap(
        width = width,
        height = height,
        palette = List(colors) { PackedColor.of(it % 256, (it * 3) % 256, (it * 7) % 256, 255) },
        indices = ByteArray(width * height) { (it % colors).toByte() },
    )

    private fun inflate(payload: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(payload)
        val buffer = ByteArray(4096)
        val out = ArrayList<Byte>()
        while (!inflater.finished()) {
            val read = inflater.inflate(buffer)
            if (read == 0) break
            repeat(read) { out.add(buffer[it]) }
        }
        inflater.end()
        return out.toByteArray()
    }

    private fun chunks(bytes: ByteArray): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        var at = SIGNATURE.size
        while (at < bytes.size) {
            val length = int(bytes, at)
            val type = String(bytes, at + 4, 4, Charsets.US_ASCII)
            val payload = bytes.copyOfRange(at + 8, at + 8 + length)
            chunks += Chunk(type = type, payload = payload, crc = int(bytes, at + 8 + length))
            at += 12 + length
        }
        return chunks
    }

    private fun int(bytes: ByteArray, at: Int): Int = (bytes[at].toInt() and 0xFF shl 24) or
        (bytes[at + 1].toInt() and 0xFF shl 16) or
        (bytes[at + 2].toInt() and 0xFF shl 8) or
        (bytes[at + 3].toInt() and 0xFF)

    private class Chunk(val type: String, val payload: ByteArray, val crc: Int)

    private companion object {
        val SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
