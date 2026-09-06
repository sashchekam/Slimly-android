package com.slimly.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Диагональная штриховка вместо превью файла.
 *
 * Честнее серого прямоугольника: видно, что это заглушка, а не «картинка
 * не загрузилась».
 */
@Composable
fun SlimlyStripes(modifier: Modifier = Modifier, style: SlimlyStripeStyle = SlimlyStripeStyle.THUMBNAIL) {
    Canvas(modifier) {
        drawRect(style.dark)

        val band = style.band.toPx()
        val period = band * 2
        val path = Path()
        var offset = -size.height

        while (offset < size.width) {
            path.moveTo(offset, size.height)
            path.lineTo(offset + size.height, 0f)
            path.lineTo(offset + size.height + band, 0f)
            path.lineTo(offset + band, size.height)
            path.close()
            offset += period
        }

        // Полосы одной фигурой, а не циклом отрисовок: у каждой команды свой
        // проход по кадру, а полос на плитке — десятки.
        drawPath(path, style.light)
    }
}

/** Крупная штриховка у плитки галереи, мелкая — у миниатюры в строке. */
enum class SlimlyStripeStyle(val light: Color, val dark: Color, val band: Dp) {
    TILE(SlimlyColor.StripeLight, SlimlyColor.StripeDark, 7.dp),
    THUMBNAIL(SlimlyColor.StripeThumbLight, SlimlyColor.StripeThumbDark, 6.dp),
}
