package com.slimly.mediakit.scan

/**
 * Готовая страница скана — ровно в том виде, в каком она ляжет в PDF.
 *
 * Форма зависит от режима, и это не деталь реализации, а разница в весе
 * документа на порядок: двухцветную страницу JPEG раздувает почти в двадцать
 * раз против упаковки по биту на пиксель, а полутоновую, наоборот, только
 * JPEG и делает лёгкой.
 */
internal sealed interface ScannedPage {
    val width: Int
    val height: Int

    /**
     * Полутоновая страница. В документ уходит теми же байтами, что здесь
     * лежат, — потоком `DCTDecode`, без перекодирования.
     */
    data class Jpeg(val data: ByteArray, override val width: Int, override val height: Int, val isGray: Boolean) :
        ScannedPage {
        // Массив байт сравнивается по ссылке, и `data class` без этого
        // обещал бы равенство, которого не даёт.
        override fun equals(other: Any?): Boolean = this === other

        override fun hashCode(): Int = System.identityHashCode(this)
    }

    /** Двухцветная страница: бит на пиксель, дальше PDF дожимает её Flate. */
    data class Bilevel(
        override val width: Int,
        override val height: Int,
        /** Строки выровнены по байту, поэтому длина строки — не `width / 8`. */
        val rowBytes: Int,
        /** Строки сверху вниз, старший бит — левый пиксель, единица — белый. */
        val bits: ByteArray,
    ) : ScannedPage {
        override fun equals(other: Any?): Boolean = this === other

        override fun hashCode(): Int = System.identityHashCode(this)
    }
}

/**
 * Страница в оттенках серого: байт на пиксель, строки сверху вниз.
 *
 * Промежуточная форма между растром и однобитной страницей PDF. Порог
 * смотрит на соседей пикселя, а соседей в готовом `Bitmap` не видно —
 * они видны только в развёрнутом буфере.
 */
internal class GrayscaleBuffer(val width: Int, val height: Int, val samples: ByteArray) {
    // Длина ровно `width * height` — выравнивания строк здесь нет.
    init {
        require(samples.size == width * height) { "Буфер полутонов не выровнен по строкам" }
    }
}
