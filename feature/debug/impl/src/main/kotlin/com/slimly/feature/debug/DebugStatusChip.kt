package com.slimly.feature.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.slimlyTap

/**
 * Метка «DEBUG» поверх приложения: горит, пока включена хоть одна подмена,
 * и по нажатию открывает меню.
 *
 * Висит над всей навигацией, а не в шапке экрана: подмена действует везде,
 * и напоминание о ней не должно зависеть от того, куда ушёл человек. Иначе
 * странное поведение сборки будут искать в коде, а не в собственном тумблере.
 */
@Composable
fun DebugStatusChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.menu_open)
    val shape = RoundedCornerShape(SlimlyRadius.Pill)

    Row(
        modifier = modifier
            .background(SlimlyColor.Accent900, shape)
            .border(width = EDGE, color = SlimlyColor.Accent600, shape = shape)
            .slimlyTap(onClick = onClick)
            .padding(horizontal = HORIZONTAL, vertical = VERTICAL)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Точка горит рядом с надписью: сама надпись могла бы сойти
        // за подпись сборки, а точка читается как «прямо сейчас включено».
        Box(Modifier.size(DOT).background(SlimlyColor.Accent, CircleShape))

        Text(text = DEBUG, style = SlimlyText.DebugTag, color = SlimlyColor.Accent200)
    }
}

private const val DEBUG = "DEBUG"
private val EDGE = 1.dp
private val HORIZONTAL = 11.dp
private val VERTICAL = 7.dp
private val DOT = 6.dp
private val GAP = 6.dp
