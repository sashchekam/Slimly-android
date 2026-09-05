package com.slimly.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slimly.designsystem.SlimlyScreenBackground

/**
 * Хаб инструментов. В фазе 0 это пустой экран с фоном Nocturne: плитки
 * появятся, когда появится первый инструмент (фаза 2).
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    SlimlyScreenBackground(modifier = modifier)
}
