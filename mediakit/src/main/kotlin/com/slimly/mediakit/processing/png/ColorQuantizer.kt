package com.slimly.mediakit.processing.png

/** Кадр, разложенный по палитре. */
internal class IndexedBitmap(
    val width: Int,
    val height: Int,
    /** Не длиннее 256 записей: столько цветов помещается в палитру PNG. */
    val palette: List<PackedColor>,
    /** По индексу палитры на пиксель, строки сверху вниз. */
    val indices: ByteArray,
)

/** Кадр → палитра не длиннее заданной. */
internal fun interface ColorQuantizing {
    /** @param colors сколько цветов оставить, 2…256. */
    fun indexed(raster: RgbaRaster, colors: Int): IndexedBitmap
}

/**
 * Медианное сечение: цветовое облако кадра делится пополам по самому
 * длинному каналу, пока коробок не станет столько, сколько нужно цветов.
 *
 * Плоская графика попадает в палитру без потерь: у логотипа и иконки
 * исходных цветов меньше её длины, и она берётся как есть. Фотография
 * и скриншот приближаются, и это честное приближение: именно им PNG
 * и «сжимают», другого рычага у формата без потерь нет.
 */
internal class MedianCutQuantizer : ColorQuantizing {
    /** Цвет с числом пикселей, которые в нём сошлись. */
    private class Sample(val color: PackedColor, val count: Int)

    /** Отрезок отсортированных образцов — коробка медианного сечения. */
    private class Box(
        val from: Int,
        val until: Int,
        val color: PackedColor,
        val count: Int,
        /** Канал, по которому коробка длиннее всего: по нему её и режут. */
        val axis: Int,
        val extent: Int,
    ) {
        /** Однопиксельную и одноцветную коробку резать нечем. */
        val isSplittable: Boolean
            get() = extent > 0 && until - from > 1

        /** Что резать раньше: широкую коробку с большим числом пикселей. */
        val priority: Long
            get() = extent.toLong() * count
    }

    override fun indexed(raster: RgbaRaster, colors: Int): IndexedBitmap {
        val limit = colors.coerceIn(MIN_COLORS, MAX_COLORS)

        exactPalette(raster, limit)?.let { exact ->
            return IndexedBitmap(
                width = raster.width,
                height = raster.height,
                palette = exact,
                indices = exactIndices(raster, exact),
            )
        }

        val palette = palette(raster, limit)
        return IndexedBitmap(
            width = raster.width,
            height = raster.height,
            palette = palette,
            indices = nearestIndices(raster, palette),
        )
    }

    // MARK: Точная палитра

    /**
     * Все цвета кадра, если их не больше [limit]. `null` — цветов больше,
     * и точной палитры не выйдет.
     *
     * Ради этой ветки всё и затевалось: логотип, иконка и скриншот с плоской
     * заливкой ложатся в палитру ровно теми цветами, что были, и «сжатие»
     * не сдвигает ни одного пикселя.
     */
    private fun exactPalette(raster: RgbaRaster, limit: Int): List<PackedColor>? {
        val seen = HashSet<Int>(limit * 2)
        val palette = ArrayList<PackedColor>(limit)

        raster.pixels.forEach { bits ->
            if (seen.add(bits)) {
                if (palette.size == limit) return null
                palette += PackedColor(bits)
            }
        }
        return palette
    }

    /**
     * Палитра собрана из самих цветов кадра, поэтому каждый пиксель находит
     * себя в ней точно, а не приближением.
     */
    private fun exactIndices(raster: RgbaRaster, palette: List<PackedColor>): ByteArray {
        val positions = HashMap<Int, Byte>(palette.size * 2)
        palette.forEachIndexed { index, color -> positions[color.bits] = index.toByte() }

        return ByteArray(raster.pixelCount) { positions[raster.pixels[it]] ?: 0 }
    }

    // MARK: Медианное сечение

    private fun palette(raster: RgbaRaster, colors: Int): List<PackedColor> {
        val samples = histogram(raster)
        if (samples.isEmpty()) return listOf(PackedColor.of(0, 0, 0, 0))

        val boxes = mutableListOf(box(samples, from = 0, until = samples.size))

        while (boxes.size < colors) {
            val target = boxes.indices
                .filter { boxes[it].isSplittable }
                .maxByOrNull { boxes[it].priority }
                ?: break

            val box = boxes.removeAt(target)
            val (left, right) = split(box, samples)
            boxes += left
            boxes += right
        }

        return boxes.map { it.color }
    }

    /**
     * Счётчики по корзинам сетки — из них и складывается цветовое облако.
     *
     * Точность гистограммы — пять бит на канал: миллион корзин — это четыре
     * мегабайта счётчиков и проход по массиву вместо словаря на двенадцати
     * мегапикселях. Три потерянных бита всё равно ушли бы в усреднение
     * по коробке.
     */
    private fun histogram(raster: RgbaRaster): MutableList<Sample> {
        val counts = IntArray(GRID_COUNT)
        raster.pixels.forEach { counts[bucket(PackedColor(it))] += 1 }

        val samples = mutableListOf<Sample>()
        counts.forEachIndexed { index, count ->
            if (count > 0) samples += Sample(color = center(index), count = count)
        }
        return samples
    }

    private fun box(samples: List<Sample>, from: Int, until: Int): Box {
        val lower = IntArray(PackedColor.CHANNELS) { BYTE }
        val upper = IntArray(PackedColor.CHANNELS)
        val sums = LongArray(PackedColor.CHANNELS)
        var total = 0

        for (index in from until until) {
            val sample = samples[index]
            total += sample.count
            for (axis in 0 until PackedColor.CHANNELS) {
                val value = sample.color.component(axis)
                lower[axis] = minOf(lower[axis], value)
                upper[axis] = maxOf(upper[axis], value)
                sums[axis] += value.toLong() * sample.count
            }
        }

        // Веса те же, что и у расстояния: коробку, где разъехалась альфа,
        // надо разрезать раньше остальных. Иначе её усреднение затянет
        // дырку в картинке полупрозрачной плёнкой.
        val spans = IntArray(PackedColor.CHANNELS) { (upper[it] - lower[it]) * SPAN_WEIGHTS[it] }
        val axis = spans.indices.maxByOrNull { spans[it] } ?: 0
        val average = IntArray(PackedColor.CHANNELS) {
            if (total > 0) minOf(BYTE, (sums[it] / total).toInt()) else 0
        }

        return Box(
            from = from,
            until = until,
            color = PackedColor.of(
                red = average[PackedColor.RED],
                green = average[PackedColor.GREEN],
                blue = average[PackedColor.BLUE],
                alpha = average[PackedColor.ALPHA],
            ),
            count = total,
            axis = axis,
            extent = spans[axis],
        )
    }

    /**
     * Режем по взвешенной медиане: пополам не по числу цветов, а по числу
     * пикселей, которые за ними стоят.
     */
    private fun split(box: Box, samples: MutableList<Sample>): Pair<Box, Box> {
        val axis = box.axis
        val sorted = samples.subList(box.from, box.until).sortedBy { it.color.component(axis) }
        sorted.forEachIndexed { offset, sample -> samples[box.from + offset] = sample }

        var running = 0
        var pivot = box.from
        for (index in box.from until box.until) {
            running += samples[index].count
            if (running * 2 >= box.count) {
                pivot = index
                break
            }
        }
        // Обе половины должны быть непустыми, иначе сечение зациклится
        // на коробке, которая не уменьшается.
        pivot = (pivot + 1).coerceIn(box.from + 1, box.until - 1)

        return box(samples, box.from, pivot) to box(samples, pivot, box.until)
    }

    // MARK: Разложение по палитре

    /**
     * Разложение с растровым приближением по Флойду — Стейнбергу.
     *
     * Ошибку округления пикселя расплёскиваем на соседей справа и снизу.
     * Без этого палитра кладёт на плавный переход ступеньки, а ступенька
     * на небе видна куда лучше, чем зерно на её месте.
     */
    private fun nearestIndices(raster: RgbaRaster, palette: List<PackedColor>): ByteArray {
        val lookup = ShortArray(GRID_COUNT) { -1 }
        val indices = ByteArray(raster.pixelCount)

        // Ошибка лежит в шестнадцатых долях и с запасом по краям строки,
        // чтобы крайним пикселям было кому её отдать.
        val stride = (raster.width + 2) * RGB_CHANNELS
        var current = IntArray(stride)
        var next = IntArray(stride)

        for (y in 0 until raster.height) {
            for (x in 0 until raster.width) {
                val pixel = y * raster.width + x
                val source = raster.color(pixel)
                val slot = (x + 1) * RGB_CHANNELS

                val wanted = PackedColor.of(
                    red = clamped(source.red + current[slot] / ERROR_SCALE),
                    green = clamped(source.green + current[slot + 1] / ERROR_SCALE),
                    blue = clamped(source.blue + current[slot + 2] / ERROR_SCALE),
                    // Альфу не размываем: зерно по краю прозрачности заметнее
                    // любой ступеньки, которую оно должно было убрать.
                    alpha = source.alpha,
                )

                val index = nearest(wanted, palette, lookup)
                indices[pixel] = index.toByte()
                spread(wanted, palette[index], slot, current, next)
            }

            val done = current
            current = next
            next = done
            next.fill(0)
        }
        return indices
    }

    /**
     * Веса Флойда — Стейнберга: семь шестнадцатых уходят вправо, остальные
     * девять — в три пикселя следующей строки.
     *
     * Разливаем половину ошибки, а не всю. Полная сила стоит дорого: она
     * засевает зерном ровные заливки, где промах по цвету и так не виден,
     * и скриншот от неё тяжелеет вчетверо.
     */
    private fun spread(wanted: PackedColor, chosen: PackedColor, slot: Int, current: IntArray, next: IntArray) {
        for (channel in 0 until RGB_CHANNELS) {
            val error = (wanted.component(channel) - chosen.component(channel)) / 2
            if (error == 0) continue

            current[slot + RGB_CHANNELS + channel] += error * ERROR_RIGHT
            next[slot - RGB_CHANNELS + channel] += error * ERROR_BELOW_LEFT
            next[slot + channel] += error * ERROR_BELOW
            next[slot + RGB_CHANNELS + channel] += error * ERROR_BELOW_RIGHT
        }
    }

    /**
     * Ближайший цвет палитры с памятью по корзинам сетки: одна и та же
     * корзина встречается в кадре тысячи раз, и перебирать палитру
     * для каждого её пикселя незачем.
     */
    private fun nearest(color: PackedColor, palette: List<PackedColor>, lookup: ShortArray): Int {
        val bucket = bucket(color)
        val known = lookup[bucket]
        if (known >= 0) return known.toInt()

        var best = 0
        var bestDistance = Int.MAX_VALUE
        palette.forEachIndexed { index, entry ->
            val distance = distance(color, entry)
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
            }
        }

        lookup[bucket] = best.toShort()
        return best
    }

    private fun clamped(value: Int): Int = value.coerceIn(0, BYTE)

    /**
     * Квадрат расстояния с весами восприятия: зелёный глаз различает лучше
     * синего, а промах по альфе виден сильнее любого промаха по цвету —
     * прозрачное пятно на месте плотного не спутать ни с чем.
     */
    private fun distance(first: PackedColor, second: PackedColor): Int {
        val red = first.red - second.red
        val green = first.green - second.green
        val blue = first.blue - second.blue
        val alpha = first.alpha - second.alpha
        return WEIGHT_RED * red * red +
            WEIGHT_GREEN * green * green +
            WEIGHT_BLUE * blue * blue +
            WEIGHT_ALPHA * alpha * alpha
    }

    // MARK: Сетка

    private fun bucket(color: PackedColor): Int {
        val red = color.red shr GRID_SHIFT
        val green = color.green shr GRID_SHIFT
        val blue = color.blue shr GRID_SHIFT
        return ((red * GRID_STEPS + green) * GRID_STEPS + blue) * GRID_STEPS + (color.alpha shr GRID_SHIFT)
    }

    /**
     * Цвет в середине корзины. Края шкалы точные: чистый чёрный и чистый
     * белый после округления должны остаться собой.
     */
    private fun center(bucket: Int): PackedColor {
        fun step(value: Int): Int = value % GRID_STEPS * BYTE / (GRID_STEPS - 1)

        return PackedColor.of(
            red = step(bucket / GRID_STEPS / GRID_STEPS / GRID_STEPS),
            green = step(bucket / GRID_STEPS / GRID_STEPS),
            blue = step(bucket / GRID_STEPS),
            alpha = step(bucket),
        )
    }

    private companion object {
        const val MIN_COLORS = 2
        const val MAX_COLORS = 256
        const val BYTE = 255
        const val RGB_CHANNELS = 3

        /** Знаменатель весов Флойда — Стейнберга и сами веса. */
        const val ERROR_SCALE = 16
        const val ERROR_RIGHT = 7
        const val ERROR_BELOW_LEFT = 3
        const val ERROR_BELOW = 5
        const val ERROR_BELOW_RIGHT = 1

        /**
         * Веса восприятия в расстоянии между цветами: зелёный глаз различает
         * лучше синего, а промах по альфе виден сильнее любого промаха
         * по цвету.
         */
        const val WEIGHT_RED = 2
        const val WEIGHT_GREEN = 4
        const val WEIGHT_BLUE = 3
        const val WEIGHT_ALPHA = 8

        /** Точность гистограммы: пять бит на каждый из четырёх каналов. */
        const val GRID_STEPS = 32
        const val GRID_SHIFT = 3
        const val GRID_COUNT = GRID_STEPS * GRID_STEPS * GRID_STEPS * GRID_STEPS

        /**
         * Во сколько раз разброс канала важнее такого же разброса красного.
         * Порядок тот же, что у весов расстояния, но без возведения в квадрат:
         * здесь сравниваются длины, а не их квадраты.
         */
        val SPAN_WEIGHTS = intArrayOf(2, 3, 2, 4)
    }
}
