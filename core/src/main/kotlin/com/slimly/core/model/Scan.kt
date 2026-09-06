package com.slimly.core.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.slimly.core.R
import kotlinx.parcelize.Parcelize
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Как обрабатываем страницу перед сборкой в PDF.
 *
 * Это не «фильтр для красоты»: от режима зависит читаемость текста. Скан
 * договора и скан чека с печатью — разные задачи, и одним режимом их
 * не закрыть.
 */
enum class ScanMode(
    /**
     * Подпись в списке форматов. Короткая: «PDF» повторяется во всех трёх
     * вариантах, и длинные хвосты превратили бы выбор в стену текста.
     */
    @get:StringRes val displayNameRes: Int,
) {
    /** Цвет сохраняем — нужен печатям, подписям и иллюстрациям. */
    COLOR(R.string.palette_color),

    /** Оттенки серого: текст читается так же, а лишний цвет не отвлекает. */
    GRAYSCALE(R.string.palette_gray),

    /** Чистая двухцветная страница — самый контрастный вариант. */
    BLACK_AND_WHITE(R.string.palette_black_and_white),
}

/** Точка в долях кадра с началом в левом верхнем углу. */
@Parcelize
data class QuadPoint(val x: Float, val y: Float) : Parcelable

/**
 * Границы листа в кадре.
 *
 * Координаты — доли кадра с началом в левом верхнем углу: рамку рисует
 * Compose, и переворачивать ось один раз на границе с системой дешевле,
 * чем помнить про неё в каждом месте отрисовки.
 */
@Parcelize
data class DocumentQuad(
    val topLeft: QuadPoint,
    val topRight: QuadPoint,
    val bottomRight: QuadPoint,
    val bottomLeft: QuadPoint,
) : Parcelable {
    /** Угол четырёхугольника — в том же порядке, в каком их обходит рамка. */
    enum class Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
    }

    /** Углы по часовой стрелке — в том порядке, в каком их обходит рамка. */
    val corners: List<QuadPoint>
        get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    operator fun get(corner: Corner): QuadPoint = corners[corner.ordinal]

    /**
     * Насколько углы разъехались с прошлым кадром — в долях кадра.
     *
     * По этому числу решается, что лист лежит спокойно: сравнивать площади
     * бесполезно, потому что при повороте вокруг центра она не меняется.
     */
    fun drift(from: DocumentQuad): Float = corners.zip(from.corners).fold(0f) { largest, (current, previous) ->
        maxOf(largest, hypot(current.x - previous.x, current.y - previous.y))
    }

    /**
     * Тот же четырёхугольник с одним переставленным углом.
     *
     * `null` — угол занесли за соседние стороны: рамка сложилась бы бантиком,
     * и выпрямлять по ней было бы нечего. Отказ вместо подгонки намеренный:
     * палец продолжает вести, а рамка просто перестаёт следовать за ним —
     * это понятнее, чем угол, живущий своей жизнью.
     */
    fun moving(corner: Corner, to: QuadPoint): DocumentQuad? {
        val points = corners.toMutableList()
        points[corner.ordinal] = QuadPoint(
            x = to.x.coerceIn(0f, 1f),
            y = to.y.coerceIn(0f, 1f),
        )

        val moved = DocumentQuad(
            topLeft = points[0],
            topRight = points[1],
            bottomRight = points[2],
            bottomLeft = points[3],
        )
        return moved.takeIf { it.isUsable }
    }

    /**
     * Выпуклый и не выродившийся в полоску: только по такой рамке можно
     * выпрямить перспективу.
     */
    val isUsable: Boolean
        get() = isConvex && area >= MINIMUM_AREA

    /** Площадь по формуле шнурования — в долях кадра. */
    private val area: Float
        get() {
            val points = corners
            val doubled = points.indices.fold(0f) { sum, index ->
                val current = points[index]
                val next = points[(index + 1) % points.size]
                sum + current.x * next.y - next.x * current.y
            }
            return abs(doubled) / 2
        }

    /** Все повороты обхода в одну сторону — значит, стороны не пересекаются. */
    private val isConvex: Boolean
        get() {
            val points = corners
            var sign = 0f

            for (index in points.indices) {
                val a = points[index]
                val b = points[(index + 1) % points.size]
                val c = points[(index + 2) % points.size]
                val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
                if (abs(cross) <= EPSILON) return false
                if (sign == 0f) {
                    sign = cross
                } else if (cross * sign < 0) {
                    return false
                }
            }
            return true
        }

    companion object {
        /** Порог «это ноль»: у трёх точек на одной прямой поворота нет. */
        private const val EPSILON = 1.1920929E-7f

        /**
         * Наименьшая доля кадра, которую ещё имеет смысл считать страницей.
         * Меньше — это уже не рамка, а схлопнутый в точку четырёхугольник,
         * из которого не выйдет ни превью, ни страницы PDF.
         */
        private const val MINIMUM_AREA = 0.02f

        /** Кадр целиком — то же, что «не обрезать». */
        val FULL = DocumentQuad(
            topLeft = QuadPoint(0f, 0f),
            topRight = QuadPoint(1f, 0f),
            bottomRight = QuadPoint(1f, 1f),
            bottomLeft = QuadPoint(0f, 1f),
        )
    }
}

/**
 * Границы страниц, с которыми собирается документ.
 *
 * Ключ — идентификатор страницы: задание на обработку носит с собой список
 * страниц, а границы к нему прилагаются отдельно. Чего в таблице нет, то
 * обработка ищет сама — так «границы по умолчанию» остаются поведением
 * обработчика, а не выдуманным на экране значением.
 */
@Parcelize
data class ScanBounds(private val quads: Map<String, DocumentQuad> = emptyMap()) : Parcelable {
    operator fun get(id: String): DocumentQuad? = quads[id]

    operator fun get(item: MediaItem): DocumentQuad? = quads[item.id]

    fun with(id: String, quad: DocumentQuad?): ScanBounds =
        ScanBounds(if (quad == null) quads - id else quads + (id to quad))

    val isEmpty: Boolean
        get() = quads.isEmpty()

    companion object {
        /** Границы ищет сама обработка. */
        val AUTOMATIC = ScanBounds()
    }
}
