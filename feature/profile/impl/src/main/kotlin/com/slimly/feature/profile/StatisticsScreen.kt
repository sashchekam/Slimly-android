package com.slimly.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slimly.core.model.ProcessingKind
import com.slimly.designsystem.SlimlyCard
import com.slimly.designsystem.SlimlyChartTint
import com.slimly.designsystem.SlimlyChip
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyKicker
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyScreen
import com.slimly.designsystem.SlimlyShare
import com.slimly.designsystem.SlimlyShareBar
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlySparkline
import com.slimly.designsystem.SlimlyText

/**
 * Подробная статистика: срез времени, доли инструментов и карточка на каждый.
 *
 * Экран отвечает на «чем я пользуюсь и что это дало», поэтому цифра здесь
 * всегда идёт вместе с фразой: одно число без пояснения не говорит ничего.
 */
@Composable
internal fun StatisticsScreen(viewModel: StatisticsViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(viewModel) { viewModel.load() }

    SlimlyScreen(modifier = modifier, onBack = viewModel::goBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SlimlySpacing.ScreenHorizontal)
                .padding(bottom = SlimlySpacing.X6),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X6),
        ) {
            Header(viewModel)
            RangePicker(viewModel)

            if (viewModel.isEmpty) {
                SlimlyCard {
                    Text(text = viewModel.emptyText, style = SlimlyText.Body, color = SlimlyColor.Neutral400)
                }
            } else {
                OperationsCard(viewModel)
                Column(verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X3)) {
                    viewModel.cards.forEach { ToolCardView(it) }
                }
            }

            Text(
                text = stringResource(R.string.statistics_privacy),
                style = SlimlyText.CaptionSmall,
                color = SlimlyColor.Neutral600,
            )
        }
    }
}

@Composable
private fun Header(viewModel: StatisticsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X2)) {
        Text(
            text = stringResource(R.string.statistics_title),
            style = SlimlyText.LargeTitle,
            color = SlimlyColor.Text,
        )
        Text(text = viewModel.periodText, style = SlimlyText.BodySmall, color = SlimlyColor.Neutral500)
    }
}

@Composable
private fun RangePicker(viewModel: StatisticsViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2)) {
        viewModel.ranges.forEach { range ->
            SlimlyChip(
                title = viewModel.title(range),
                isSelected = range == viewModel.range,
                // Равные доли ряда: срезов три, и выбирают между ними
                // по положению, а не по длине надписи.
                modifier = Modifier.weight(1f),
                onClick = { viewModel.select(range) },
            )
        }
    }
}

// MARK: Операции по инструментам

@Composable
private fun OperationsCard(viewModel: StatisticsViewModel) {
    SlimlyCard {
        SlimlyKicker(stringResource(R.string.statistics_by_tool))

        Row(
            modifier = Modifier.padding(top = SlimlySpacing.X3),
            horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(text = viewModel.totalText, style = SlimlyText.StatM, color = SlimlyColor.Text)
            Text(
                text = stringResource(R.string.statistics_total),
                style = SlimlyText.BodySmall,
                color = SlimlyColor.Neutral400,
                modifier = Modifier.padding(bottom = BASELINE),
            )
        }

        SlimlyShareBar(
            shares = viewModel.shares.map { SlimlyShare(fraction = it.fraction, color = shareColor(it.kind)) },
            modifier = Modifier.padding(top = SlimlySpacing.X4),
        )

        Legend(viewModel)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(viewModel: StatisticsViewModel) {
    FlowRow(
        modifier = Modifier.padding(top = SlimlySpacing.X4),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X6),
        verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
    ) {
        viewModel.shares.forEach { share ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendDot(shareColor(share.kind))
                Text(
                    text = "${share.label} · ${share.percentText}",
                    style = SlimlyText.CaptionSmall,
                    color = SlimlyColor.Neutral400,
                )
            }
        }
    }
}

/** Квадратик легенды — тот же цвет, что и у куска полосы над ней. */
@Composable
private fun LegendDot(color: Color) {
    Box(
        Modifier
            .size(LEGEND_DOT)
            .clip(RoundedCornerShape(LEGEND_RADIUS))
            .background(color),
    )
}

/**
 * Цвета идут по убыванию «громкости» в том же порядке, в каком инструменты
 * стоят на главной: полоса и легенда читаются как один список, а не как
 * случайный набор оттенков.
 */
private fun shareColor(kind: ProcessingKind): Color = when (kind) {
    ProcessingKind.COMPRESSION -> SlimlyColor.Accent500
    ProcessingKind.CONVERSION -> SlimlyColor.Accent700
    ProcessingKind.AUDIO_EXTRACTION -> SlimlyColor.Neutral600
    ProcessingKind.SCAN -> SlimlyColor.Neutral800
}

// MARK: Карточка инструмента

@Composable
private fun ToolCardView(card: ToolCard) {
    SlimlyCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
            ) {
                Text(text = card.title, style = SlimlyText.SectionTitle, color = SlimlyColor.Text)
                Text(text = card.note, style = SlimlyText.Caption, color = SlimlyColor.Neutral500)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = card.countText,
                    style = SlimlyText.StatCard,
                    color = if (card.isLeading) SlimlyColor.Accent200 else SlimlyColor.Neutral100,
                )
                Text(text = card.unit, style = SlimlyText.Micro, color = SlimlyColor.Neutral500)
            }
        }

        SlimlySparkline(
            values = card.spark,
            modifier = Modifier.padding(top = SlimlySpacing.X4),
            tint = if (card.isLeading) SlimlyChartTint.ACCENT else SlimlyChartTint.NEUTRAL,
        )

        Facts(card.facts)
    }
}

@Composable
private fun Facts(facts: List<ToolFact>) {
    // Просвет в один пункт вместо разделителей — тот же приём, что в списке
    // строк: линия получается сплошной внутри блока.
    Row(
        modifier = Modifier
            .padding(top = SlimlySpacing.X4)
            .fillMaxWidth()
            .clip(RoundedCornerShape(SlimlyRadius.Sm))
            .background(SlimlyColor.Text.copy(alpha = SEAM_ALPHA)),
        horizontalArrangement = Arrangement.spacedBy(SEAM),
    ) {
        facts.forEach { fact ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(SlimlyColor.SurfaceInset)
                    .padding(horizontal = FACT_HORIZONTAL, vertical = FACT_VERTICAL),
                verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X1),
            ) {
                Text(text = fact.value, style = SlimlyText.RowTitle, color = SlimlyColor.Neutral100)
                Text(text = fact.label, style = SlimlyText.Nano, color = SlimlyColor.Neutral500)
            }
        }
    }
}

private val BASELINE = 2.dp
private val LEGEND_DOT = 7.dp
private val LEGEND_RADIUS = 2.dp
private val SEAM = 1.dp
private val FACT_HORIZONTAL = 11.dp
private val FACT_VERTICAL = 9.dp
private const val SEAM_ALPHA = 0.08f
