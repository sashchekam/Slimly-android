package com.slimly.core.format

import android.content.res.Resources
import java.text.NumberFormat

interface CountFormatting {
    /** Крупные количества с разрядами: «12 480», «12,480». */
    fun string(count: Int): String

    /** «1 файл», «3 файла», «128 файлов». */
    fun string(count: Int, noun: CountNoun): String

    /**
     * Одно слово, без числа: в профиле цифра набирается крупно, а подпись
     * под ней мелко, и склеить их в одну строку нельзя.
     */
    fun word(count: Int, noun: CountNoun): String
}

/**
 * Количества словами.
 *
 * Форма существительного согласуется с числом по правилам языка, а правила
 * эти везде свои: у английского две формы, у русского три, у японского одна.
 * Выбор отдан каталогу строк — там у каждого языка свой набор вариантов,
 * и подбирать его таблицей в коде не нужно.
 */
class CountFormatter(private val resources: Resources) : CountFormatting {
    override fun string(count: Int): String = NumberFormat.getIntegerInstance(resources.locale).format(count)

    override fun string(count: Int, noun: CountNoun): String = resources.getQuantityString(noun.countRes, count, count)

    override fun word(count: Int, noun: CountNoun): String = resources.getQuantityString(noun.wordRes, count)
}
