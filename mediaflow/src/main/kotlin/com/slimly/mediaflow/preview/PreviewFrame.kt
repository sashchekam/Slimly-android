package com.slimly.mediaflow.preview

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyStripeStyle
import com.slimly.designsystem.SlimlyStripes
import com.slimly.designsystem.SlimlyText

/**
 * Окно просмотра: кадр поверх штриховки, а пока кадра нет — подпись,
 * что именно грузится.
 *
 * Штриховка честнее серого прямоугольника: видно, что это заглушка,
 * а не «картинка не загрузилась».
 */
@Composable
internal fun PreviewFrame(
    frame: Bitmap?,
    placeholder: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        SlimlyStripes(Modifier.fillMaxSize(), SlimlyStripeStyle.TILE)

        if (frame == null) {
            Text(
                text = placeholder,
                style = SlimlyText.Kicker,
                color = SlimlyColor.Neutral600,
                textAlign = TextAlign.Center,
            )
        } else {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}
