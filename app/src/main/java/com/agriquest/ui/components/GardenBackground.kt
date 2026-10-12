package com.agriquest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.agriquest.R
import kotlin.math.roundToInt

/** Trim the artwork's rounded bottom edge so blue cannot show through the grass. */
@Composable
fun GardenBackground(modifier: Modifier = Modifier) {
    val artwork = ImageBitmap.imageResource(R.drawable.start_bg)
    val grassBottom = artwork.height - (artwork.height * 0.035f).roundToInt()
    Canvas(modifier) {
        drawImage(
            image = artwork,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(artwork.width, grassBottom),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.None
        )
    }
}
