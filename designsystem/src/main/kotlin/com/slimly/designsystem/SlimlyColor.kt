package com.slimly.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Палитра Nocturne: тёмная основа, один блюрпл-акцент.
 *
 * Числа скопированы из `Colors.swift` на iOS — палитра у платформ общая,
 * и расходиться ей нельзя. Полные рампы 100–900 приезжают в фазе 1 вместе
 * с остальными токенами; здесь только роли, которые нужны фону.
 */
object SlimlyColor {
    val Background = Color(0xFF161826)
    val Surface = Color(0xFF232532)
    val Text = Color(0xFFE9E9ED)
    val Accent = Color(0xFF9184D9)

    /** Тёплое пятно в левом верхнем углу экрана — единственный источник «света» на фоне. */
    val BackgroundGlow = Color(0xFF1E2134)
}
