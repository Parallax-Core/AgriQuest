package com.agriquest.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Shared dimensions for every page; compact spacing is for content inside cards. */
object AppLayout {
    val maxWidth = 430.dp
    val pagePadding = 10.dp
    val sectionSpacing = 8.dp
    val cardPadding = 10.dp
    val compactSpacing = 8.dp
    val buttonShape = RoundedCornerShape(3.dp)
    val highlightShape = RoundedCornerShape(12.dp)
    val plantBackgroundShape = RoundedCornerShape(10.dp)
    val cardShape = androidx.compose.foundation.shape.CutCornerShape(4.dp)
    val cardElevation = 0.dp
    val cardBorderWidth = 1.5.dp
}
