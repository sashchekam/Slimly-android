package com.slimly.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyRowGroup
import com.slimly.designsystem.SlimlyScreenBackground
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.slimlyRow
import com.slimly.designsystem.slimlyTap
import com.slimly.shortcuts.ToolCatalog
import com.slimly.shortcuts.ToolShortcut
import kotlinx.coroutines.launch
import com.slimly.shortcuts.R as ShortcutsR

/**
 * Какой инструмент открывает виджет.
 *
 * Система показывает этот экран при добавлении виджета и при его настройке —
 * своей точки входа у него нет. Отсюда и вид: список каталога и ничего
 * больше, потому что настраивать в ярлыке больше нечего.
 */
class ToolWidgetConfigureActivity : ComponentActivity() {
    private val widgetId: Int
        get() = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Уход мимо выбора — это отказ: система должна убрать виджет,
        // а не оставить его настроенным наугад.
        setResult(Activity.RESULT_CANCELED, result())

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent { ToolPicker(onPick = ::pick) }
    }

    /**
     * Выбор уезжает в состояние самого виджета, а не в общие настройки:
     * виджетов на экране бывает несколько, и у каждого свой инструмент.
     */
    private fun pick(tool: ToolShortcut) {
        lifecycleScope.launch {
            val widget = ToolWidget()
            val glanceId = GlanceAppWidgetManager(this@ToolWidgetConfigureActivity).getGlanceIdBy(widgetId)

            updateAppWidgetState(this@ToolWidgetConfigureActivity, glanceId) { it[toolKey] = tool.id.code }
            widget.update(this@ToolWidgetConfigureActivity, glanceId)

            setResult(Activity.RESULT_OK, result())
            finish()
        }
    }

    private fun result(): Intent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
}

@Composable
private fun ToolPicker(onPick: (ToolShortcut) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        SlimlyScreenBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(SlimlySpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
        ) {
            item {
                Text(
                    text = stringResource(ShortcutsR.string.widget_tool_title),
                    style = SlimlyText.LargeTitle,
                    color = SlimlyColor.Text,
                    modifier = Modifier.padding(top = SlimlySpacing.X4),
                )
            }
            item {
                Text(
                    text = stringResource(ShortcutsR.string.widget_tool_configure),
                    style = SlimlyText.Body,
                    color = SlimlyColor.Neutral500,
                )
            }
            item { ToolRows(onPick) }
        }
    }
}

@Composable
private fun ToolRows(onPick: (ToolShortcut) -> Unit) {
    SlimlyRowGroup {
        ToolCatalog.AVAILABLE.forEach { tool ->
            Row(
                modifier = Modifier
                    .slimlyTap { onPick(tool) }
                    .slimlyRow(),
                horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(tool.icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(SlimlyColor.Neutral300),
                    modifier = Modifier.size(ICON),
                )

                Column(Modifier.padding(vertical = SlimlySpacing.X1)) {
                    Text(
                        text = stringResource(tool.title),
                        style = SlimlyText.RowTitle,
                        color = SlimlyColor.Text,
                    )
                    Text(
                        text = stringResource(tool.subtitle),
                        style = SlimlyText.CaptionSmall,
                        color = SlimlyColor.Neutral500,
                        modifier = Modifier.padding(top = SlimlySpacing.X1),
                    )
                }
            }
        }
    }
}

private val ICON = 24.dp
