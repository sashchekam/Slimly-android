package com.slimly.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.slimly.designsystem.SlimlyColor
import com.slimly.shortcuts.ToolCatalog
import com.slimly.shortcuts.ToolShortcut

/** Где виджет помнит выбранный инструмент — кодом из каталога. */
internal val toolKey = stringPreferencesKey("slimly.widget.tool")

/**
 * Один инструмент на выбор человека.
 *
 * Второй виджет вместо пяти отдельных: на экран можно положить сразу
 * несколько ярлыков и каждый настроить по-своему, а список в галерее
 * виджетов остаётся коротким.
 *
 * Инструмент лежит в состоянии самого виджета, а не в общих настройках
 * приложения: виджетов на экране бывает несколько, и выбор у каждого свой.
 */
class ToolWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                // Виджет только что поставили и ещё не настроили — показываем
                // первый инструмент каталога, а не пустую плитку.
                ToolTile(ToolCatalog.named(currentState(toolKey).orEmpty()) ?: ToolCatalog.AVAILABLE.first())
            }
        }
    }
}

class ToolWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ToolWidget()
}

/**
 * Карточка хаба, растянутая на плитку: та же иконка, то же имя, та же
 * подпись — чтобы ярлык узнавался с первого взгляда.
 */
@Composable
private fun ToolTile(tool: ToolShortcut) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(SlimlyColor.Background)
            .padding(PADDING)
            .clickable(actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tool.url)))),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            provider = ImageProvider(tool.icon),
            contentDescription = context.getString(tool.title),
            colorFilter = ColorFilter.tint(ColorProvider(SlimlyColor.Neutral300)),
            modifier = GlanceModifier.size(ICON),
        )

        Spacer(GlanceModifier.defaultWeight())

        Text(
            text = context.getString(tool.title),
            style = TextStyle(color = ColorProvider(SlimlyColor.Text), fontSize = TITLE),
            modifier = GlanceModifier.wrapContentHeight(),
        )
        Text(
            text = context.getString(tool.subtitle),
            style = TextStyle(color = ColorProvider(SlimlyColor.Neutral500), fontSize = SUBTITLE),
            modifier = GlanceModifier.padding(top = SUBTITLE_TOP).wrapContentHeight(),
        )
    }
}

private val PADDING = 14.dp
private val ICON = 26.dp
private val SUBTITLE_TOP = 3.dp
private val TITLE = 14.sp
private val SUBTITLE = 11.sp
