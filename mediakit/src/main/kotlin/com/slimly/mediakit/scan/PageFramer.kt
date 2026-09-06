package com.slimly.mediakit.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Size
import com.slimly.core.model.DocumentQuad
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Достать страницу из снимка: выпрямить лист, вернуть ему истинные пропорции
 * и привести к рабочему разрешению.
 *
 * Одна работа — «где на снимке страница, какой она формы и какого размера».
 * Порознь шаги не имеют смысла: пропорции считаются по тем же углам, что
 * и выпрямление, а масштаб — уже по исправленным пропорциям.
 */
internal fun interface PageFraming {
    /**
     * @param quad границы листа, выбранные до обработки. `null` — их никто
     *   не задавал, и обрезать наугад мы не станем: лучше оставить поля,
     *   чем срезать половину страницы.
     */
    fun page(frame: Bitmap, quad: DocumentQuad?, resolution: PageResolution): Bitmap
}

internal class PageFramer(private val aspects: PageAspectEstimating = PageAspectEstimator()) : PageFraming {
    override fun page(frame: Bitmap, quad: DocumentQuad?, resolution: PageResolution): Bitmap {
        if (quad == null) return resized(frame, resolution, aspect = 1f)

        val straightened = corrected(frame, quad)
        val aspect = correction(straightened, quad, frame.width, frame.height)
        return resized(straightened, resolution, aspect)
    }

    // MARK: Ракурс

    /**
     * Выпрямление четырёхугольника в прямоугольник.
     *
     * `setPolyToPoly` на четырёх точках даёт именно перспективное
     * преобразование — то же, что `CIPerspectiveCorrection` на iOS.
     * Размер результата берём по длинам сторон: их перекос исправляет
     * следующий шаг, по восстановленным пропорциям.
     */
    private fun corrected(frame: Bitmap, quad: DocumentQuad): Bitmap {
        val width = frame.width.toFloat()
        val height = frame.height.toFloat()
        val source = floatArrayOf(
            quad.topLeft.x * width,
            quad.topLeft.y * height,
            quad.topRight.x * width,
            quad.topRight.y * height,
            quad.bottomRight.x * width,
            quad.bottomRight.y * height,
            quad.bottomLeft.x * width,
            quad.bottomLeft.y * height,
        )

        val side = sides(source)
        if (side.width < 1 || side.height < 1) return frame

        val target = floatArrayOf(
            0f,
            0f,
            side.width.toFloat(),
            0f,
            side.width.toFloat(),
            side.height.toFloat(),
            0f,
            side.height.toFloat(),
        )

        val matrix = Matrix()
        if (!matrix.setPolyToPoly(source, 0, target, 0, CORNERS)) return frame

        val page = Bitmap.createBitmap(side.width, side.height, Bitmap.Config.ARGB_8888)
        Canvas(page).apply {
            // Белая подложка: у наклонённого листа углы кадра могут остаться
            // непокрытыми, и прозрачность там читалась бы как чёрные клинья.
            drawColor(Color.WHITE)
            drawBitmap(frame, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }
        return page
    }

    /** Размер выпрямленного листа: по длинным из противоположных сторон. */
    private fun sides(points: FloatArray): Size {
        fun length(from: Int, to: Int) = hypot(points[to] - points[from], points[to + 1] - points[from + 1])

        val width = max(length(0, 2), length(6, 4))
        val height = max(length(0, 6), length(2, 4))
        return Size(width.roundToInt(), height.roundToInt())
    }

    /**
     * Во сколько раз растянуть выпрямленный лист по ширине, чтобы он стал
     * той формы, какой лежал на столе.
     *
     * Выпрямление снимает наклон, но размер готовой картинки берёт по длинам
     * сторон четырёхугольника как они есть — а дальний край листа в кадре
     * короче ближнего. Ошибка доходит до четверти ширины и на глаз читается
     * как «буквы растянуло».
     */
    private fun correction(page: Bitmap, quad: DocumentQuad, frameWidth: Int, frameHeight: Int): Float {
        val wanted = aspects.ratio(quad, frameWidth, frameHeight) ?: return 1f
        if (page.height <= 0) return 1f
        val actual = page.width.toFloat() / page.height
        return if (actual > 0) wanted / actual else 1f
    }

    // MARK: Размер

    private fun resized(page: Bitmap, resolution: PageResolution, aspect: Float): Bitmap {
        // Длинную сторону считаем уже по исправленной форме: растягивание
        // по ширине меняет то, какая из сторон длиннее.
        val side = max(page.width * aspect, page.height.toFloat())
        if (side <= 1) return page

        // Растягиваем только в разрешённых пределах: новых букв интерполяция
        // не рисует, а вес страницы растит честно.
        val scale = min(resolution.pageSide / side, resolution.maxUpscale)
        if (abs(scale - 1) <= TOLERANCE && abs(aspect - 1) <= TOLERANCE) return page

        val width = max(1f, page.width * scale * aspect).roundToInt()
        val height = max(1f, page.height * scale).roundToInt()
        // Ланцоша в системе нет — уменьшаем ступенями по два раза.
        // Одношаговая билинейная интерполяция на большом уменьшении
        // усредняет штрих буквы с бумагой, и строчка расплывается в полосу.
        return halved(page, width, height)
    }

    /**
     * Уменьшение вдвое за шаг, пока до цели не останется меньше двух раз.
     *
     * Каждый шаг билинейной интерполяции смотрит на четыре соседних пикселя,
     * поэтому уменьшение вдвое учитывает все исходные, а не выбрасывает
     * три четверти из них.
     */
    private fun halved(source: Bitmap, width: Int, height: Int): Bitmap {
        var current = source

        while (current.width >= width * 2 && current.height >= height * 2) {
            val next = Bitmap.createScaledBitmap(current, current.width / 2, current.height / 2, true)
            if (current !== source) current.recycle()
            current = next
        }

        val result = Bitmap.createScaledBitmap(current, width, height, true)
        if (current !== source && current !== result) current.recycle()
        return result
    }

    private companion object {
        const val CORNERS = 4

        /**
         * Насколько преобразование должно разойтись с тождественным, чтобы
         * пересчёт имел смысл. Прогон ради процента размера — только лишняя
         * интерполяция.
         */
        const val TOLERANCE = 0.005f
    }
}
