package com.slimly.feature.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyText

/**
 * Метка сборки: моноширинная надпись в акцентной рамке.
 *
 * Служебность здесь подчёркнута нарочно — так и метка «INTERNAL» в шапке
 * меню, и «DEBUG» поверх приложения читаются как чужие продукту, а не как
 * его часть.
 */
@Composable
internal fun DebugTag(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = SlimlyText.DebugTag,
        color = SlimlyColor.Accent200,
        modifier = modifier
            .background(SlimlyColor.Accent900, RoundedCornerShape(RADIUS))
            .padding(horizontal = HORIZONTAL, vertical = VERTICAL),
    )
}

private val RADIUS = 5.dp
private val HORIZONTAL = 6.dp
private val VERTICAL = 3.dp
