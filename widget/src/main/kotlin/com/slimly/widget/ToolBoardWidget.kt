package com.slimly.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.slimly.designsystem.SlimlyColor
import com.slimly.shortcuts.ToolCatalog
import com.slimly.shortcuts.ToolShortcut

/**
 * Полка инструментов: все ярлыки сразу, каждый открывает свой.
 *
 * Виджет здесь — ярлык: он ничего не считает и ни за чем не следит, а только
 * открывает инструмент по адресу `slimly://`. Поэтому у него нет ни обновления
 * по расписанию, ни доступа к данным приложения.
 */
class ToolBoardWidget : GlanceAppWidget() {
    /**
     * Раскладка выбирается по размеру. На узком виджете инструменты стоят
     * рядом значков: карточка с подписью в такую высоту не влезает,
     * а обрезанная подпись хуже её отсутствия.
     */
    override val sizeMode = SizeMode.Responsive(setOf(ROW, BOARD))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                ToolBoard()
            }
        }
    }

    private companion object {
        val ROW = DpSize(180.dp, 70.dp)
        val BOARD = DpSize(180.dp, 150.dp)
    }
}

class ToolBoardReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ToolBoardWidget()
}

@Composable
private fun ToolBoard() {
    val isTall = LocalSize.current.height >= TALL_FROM

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(SlimlyColor.Background)
            .padding(PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            ToolCatalog.AVAILABLE.forEach { tool ->
                ToolTile(tool = tool, showsTitle = isTall, modifier = GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
private fun ToolTile(tool: ToolShortcut, showsTitle: Boolean, modifier: GlanceModifier = GlanceModifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .padding(TILE_GAP)
            .background(SlimlyColor.Surface)
            .cornerRadius(TILE_RADIUS)
            .padding(vertical = TILE_VERTICAL)
            .clickable(actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tool.url)))),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(tool.icon),
            contentDescription = context.getString(tool.title),
            colorFilter = ColorFilter.tint(ColorProvider(SlimlyColor.Neutral300)),
            modifier = GlanceModifier.size(ICON),
        )

        if (showsTitle) {
            Text(
                text = context.getString(tool.short),
                style = TextStyle(color = ColorProvider(SlimlyColor.Neutral400), fontSize = LABEL),
                modifier = GlanceModifier.padding(top = LABEL_TOP).wrapContentHeight(),
            )
        }
    }
}

/** Отсюда виджет считается высоким: под значком помещается ещё и подпись. */
private val TALL_FROM = 100.dp
private val PADDING = 8.dp
private val TILE_GAP = 4.dp
private val TILE_RADIUS = 10.dp
private val TILE_VERTICAL = 12.dp
private val ICON = 22.dp
private val LABEL_TOP = 6.dp
private val LABEL = 10.sp
