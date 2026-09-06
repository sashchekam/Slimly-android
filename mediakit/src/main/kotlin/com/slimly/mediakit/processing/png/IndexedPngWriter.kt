package com.slimly.mediakit.processing.png

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * Кадр с палитрой → байты PNG.
 *
 * Своя запись нужна потому, что `Bitmap.compress` палитру не умеет: он всегда
 * кладёт truecolor, и картинка из четырёх цветов весит у него столько же,
 * сколько фотография того же размера.
 */
internal class IndexedPngWriter {
    /** `null` — писать нечего: пустая палитра или пустой кадр. */
    fun bytes(bitmap: IndexedBitmap): ByteArray? {
        if (bitmap.palette.isEmpty() || bitmap.width <= 0 || bitmap.height <= 0) return null

        val sorted = sortedByAlpha(bitmap)
        val depth = bitDepth(sorted.palette.size)
        val compressed = deflate(rows(sorted, depth))

        val out = ByteArrayOutputStream()
        out.write(SIGNATURE)
        out.write(chunk(HEADER, header(sorted.width, sorted.height, depth)))
        out.write(chunk(PALETTE, sorted.palette.flatMap { listOf(it.red, it.green, it.blue) }.toBytes()))
        transparency(sorted.palette)?.let { out.write(chunk(TRANSPARENCY, it)) }
        out.write(chunk(DATA, compressed))
        out.write(chunk(END, ByteArray(0)))
        return out.toByteArray()
    }

    // MARK: Палитра

    /**
     * Полупрозрачные записи уезжают в начало палитры.
     *
     * Чанк tRNS обрывается на последней непрозрачной записи, поэтому от
     * порядка зависит его длина: у картинки с одной дыркой в углу это байт
     * вместо двухсот пятидесяти шести.
     */
    private fun sortedByAlpha(bitmap: IndexedBitmap): IndexedBitmap {
        val order = bitmap.palette.indices.sortedBy { bitmap.palette[it].alpha }
        if (order == bitmap.palette.indices.toList()) return bitmap

        val positions = ByteArray(bitmap.palette.size)
        order.forEachIndexed { position, source -> positions[source] = position.toByte() }

        return IndexedBitmap(
            width = bitmap.width,
            height = bitmap.height,
            palette = order.map { bitmap.palette[it] },
            indices = ByteArray(bitmap.indices.size) { positions[bitmap.indices[it].toInt() and BYTE] },
        )
    }

    /**
     * Альфа записей до последней полупрозрачной. Полностью непрозрачной
     * палитре чанк не нужен вовсе.
     */
    private fun transparency(palette: List<PackedColor>): ByteArray? {
        val last = palette.indexOfLast { !it.isOpaque }
        if (last < 0) return null

        return ByteArray(last + 1) { palette[it].alpha.toByte() }
    }

    /**
     * Индексу нужно ровно столько бит, сколько занимает длина палитры:
     * четырёхцветной иконке хватает двух бит на пиксель вместо восьми.
     */
    private fun bitDepth(colors: Int): Int = DEPTHS.firstOrNull { colors <= 1 shl it } ?: BITS_IN_BYTE

    // MARK: Пиксели

    /**
     * Строки со служебным байтом фильтра в начале каждой.
     *
     * Фильтр всегда нулевой: он предсказывает байт по соседям, а у индексов
     * соседство ничего не значит — разница между «цветом 3» и «цветом 200»
     * не число, и вычитать их бессмысленно.
     */
    private fun rows(bitmap: IndexedBitmap, depth: Int): ByteArray {
        val perByte = BITS_IN_BYTE / depth
        val stride = (bitmap.width + perByte - 1) / perByte
        val rows = ByteArray(bitmap.height * (stride + 1))

        for (y in 0 until bitmap.height) {
            val row = y * (stride + 1) + 1
            for (x in 0 until bitmap.width) {
                val index = bitmap.indices[y * bitmap.width + x].toInt() and BYTE
                if (depth == BITS_IN_BYTE) {
                    rows[row + x] = index.toByte()
                } else {
                    // Внутри байта индексы лежат старшими битами вперёд.
                    val shift = BITS_IN_BYTE - depth * (x % perByte + 1)
                    val at = row + x / perByte
                    rows[at] = (rows[at].toInt() or (index shl shift)).toByte()
                }
            }
        }
        return rows
    }

    /**
     * Байты в zlib-обёртке — в таком виде их ждёт чанк IDAT. Ни заголовка,
     * ни Adler-32 руками писать не приходится: `Deflater` без `nowrap`
     * отдаёт готовый поток по RFC 1950.
     */
    private fun deflate(source: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        val out = ByteArrayOutputStream(source.size / 2 + BUFFER)
        val buffer = ByteArray(BUFFER)

        try {
            deflater.setInput(source)
            deflater.finish()
            while (!deflater.finished()) {
                out.write(buffer, 0, deflater.deflate(buffer))
            }
        } finally {
            deflater.end()
        }
        return out.toByteArray()
    }

    // MARK: Чанки

    private fun header(width: Int, height: Int, depth: Int): ByteArray =
        // Тип цвета 3 — палитра; дальше нулями идут метод сжатия, метод
        // фильтрации и отсутствие чересстрочности.
        width.bigEndianBytes() + height.bigEndianBytes() + byteArrayOf(depth.toByte(), PALETTE_COLOR, 0, 0, 0)

    private fun chunk(type: String, payload: ByteArray): ByteArray {
        val body = type.toByteArray(Charsets.US_ASCII) + payload
        val crc = CRC32().apply { update(body) }.value.toInt()
        return payload.size.bigEndianBytes() + body + crc.bigEndianBytes()
    }

    /** Четыре байта старшим вперёд — порядок, в котором числа лежат в PNG. */
    private fun Int.bigEndianBytes(): ByteArray =
        ByteArray(WORD) { (this ushr (BITS_IN_BYTE * (WORD - 1 - it))).toByte() }

    private fun List<Int>.toBytes(): ByteArray = ByteArray(size) { this[it].toByte() }

    private companion object {
        val SIGNATURE = byteArrayOf(
            0x89.toByte(),
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A,
        )

        const val HEADER = "IHDR"
        const val PALETTE = "PLTE"
        const val TRANSPARENCY = "tRNS"
        const val DATA = "IDAT"
        const val END = "IEND"

        /** Тип цвета PNG: изображение с палитрой. */
        const val PALETTE_COLOR: Byte = 3

        const val BITS_IN_BYTE = 8
        const val BYTE = 0xFF
        const val WORD = 4
        const val BUFFER = 16 * 1024

        /**
         * Глубины индекса, которые разрешает PNG: четырёхцветной иконке
         * хватает двух бит на пиксель вместо восьми.
         */
        val DEPTHS = listOf(1, 2, 4, 8)
    }
}
