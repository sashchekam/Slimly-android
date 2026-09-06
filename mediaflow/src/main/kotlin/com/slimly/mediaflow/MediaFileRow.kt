package com.slimly.mediaflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slimly.core.model.MediaItem
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText

/**
 * Строка файла в списке: кадр, имя и то, что о файле важно на этом шаге.
 *
 * Кадр маленький — по нему видно, что файл выбран, но не то, что на нём;
 * подробности живут во второй строке.
 */
@Composable
fun MediaFileRow(
    item: MediaItem,
    name: String,
    detail: String,
    thumbnails: MediaThumbnailLoader,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        MediaThumbnail(
            item = item,
            thumbnails = thumbnails,
            size = THUMBNAIL_SIZE,
            modifier = Modifier
                .size(THUMBNAIL_SIDE)
                .clip(RoundedCornerShape(SlimlyRadius.Thumbnail)),
        )

        Column(
            modifier = Modifier.padding(start = SlimlySpacing.X3),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X1),
        ) {
            Text(
                text = name,
                style = SlimlyText.RowTitle,
                color = SlimlyColor.Text,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
            Text(text = detail, style = SlimlyText.Caption, color = SlimlyColor.Neutral500)
        }
    }
}

private const val THUMBNAIL_SIZE = 128
private val THUMBNAIL_SIDE = 33.dp
