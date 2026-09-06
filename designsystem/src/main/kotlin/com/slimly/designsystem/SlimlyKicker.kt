package com.slimly.designsystem

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Надпись над блоком: «ОСВОБОЖДЕНО ВСЕГО», «ЧЕМ ПОЛЬЗОВАЛИСЬ», «ГОТОВО».
 *
 * Регистр ставит показ, а не строка в каталоге: в каталоге лежит обычная
 * фраза, и в турецком или немецком её верхний регистр считается по правилам
 * языка, а не по латинской таблице.
 */
@Composable
fun SlimlyKicker(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = SlimlyText.Kicker,
        color = SlimlyColor.Neutral500,
        modifier = modifier,
    )
}
