package com.slimly.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Пилюля с дельтой сжатия: «−64%». */
@Composable
fun SlimlyDeltaBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = SlimlyText.Pill,
        color = SlimlyColor.Accent300,
        modifier = modifier
            .background(SlimlyColor.Accent900, RoundedCornerShape(SlimlyRadius.Pill))
            .padding(horizontal = DELTA_HORIZONTAL, vertical = DELTA_VERTICAL),
    )
}

/**
 * Подпись поверх кадра: «ОРИГИНАЛ», «после · 1,2 МБ».
 *
 * Фон непрозрачен настолько, чтобы надпись читалась и на белом листе,
 * и на ночном снимке: подпись поверх кадра иначе живёт только на одном
 * из них.
 */
@Composable
fun SlimlyFrameCaption(text: String, modifier: Modifier = Modifier, color: Color = SlimlyColor.Neutral300) {
    Text(
        text = text,
        style = SlimlyText.Pill,
        color = color,
        modifier = modifier
            .background(SlimlyColor.TileCaption, RoundedCornerShape(SlimlyRadius.Pill))
            .padding(horizontal = CAPTION_HORIZONTAL, vertical = CAPTION_VERTICAL),
    )
}

private val DELTA_HORIZONTAL = 7.dp
private val DELTA_VERTICAL = 3.dp
private val CAPTION_HORIZONTAL = 9.dp
private val CAPTION_VERTICAL = 4.dp
