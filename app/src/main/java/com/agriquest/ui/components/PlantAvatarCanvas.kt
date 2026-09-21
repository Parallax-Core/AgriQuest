package com.agriquest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Amber500
import com.agriquest.ui.theme.Emerald200
import com.agriquest.ui.theme.Emerald500
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.Slate900
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom 2D Canvas Vector Illustration of a potted tomato plant avatar.
 * Renders distinct illustrations for each [PlantGrowthStage]:
 * - Stage 0 (SEEDLING_SPROUT): Small green sprout emerging from soil pot with two tiny leaves.
 * - Stage 1 (VEGETATIVE_LEAFY): Medium leafy stem with vibrant green branches.
 * - Stage 2 (FLOWERING): Taller plant with yellow tomato blossom flowers.
 * - Stage 3 (FRUITING_HARVEST): Full lush plant with bright red ripe tomatoes and green stems.
 */
@Composable
fun PlantAvatarCanvas(
    modifier: Modifier = Modifier,
    growthStage: PlantGrowthStage = PlantGrowthStage.SEEDLING_SPROUT,
    size: Dp = 140.dp
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        val width = size.toPx()
        val height = size.toPx()
        val centerX = width / 2f

        // --- 1. Common Terracotta Pot ---
        val potTopY = height * 0.68f
        val potBottomY = height * 0.94f
        val potTopWidth = width * 0.52f
        val potBottomWidth = width * 0.38f
        val rimHeight = height * 0.08f

        // Soil
        drawOval(
            color = Color(0xFF4A3525),
            topLeft = Offset(centerX - (potTopWidth / 2f) + 4f, potTopY - 4f),
            size = Size(potTopWidth - 8f, rimHeight)
        )

        // Pot Body Trapezoid
        val potBodyPath = Path().apply {
            moveTo(centerX - (potTopWidth / 2f) + 6f, potTopY + rimHeight)
            lineTo(centerX + (potTopWidth / 2f) - 6f, potTopY + rimHeight)
            lineTo(centerX + (potBottomWidth / 2f), potBottomY)
            lineTo(centerX - (potBottomWidth / 2f), potBottomY)
            close()
        }
        drawPath(
            path = potBodyPath,
            color = Color(0xFFD97736) // Terracotta base
        )

        // Pot Shading Overlay
        val potShadowPath = Path().apply {
            moveTo(centerX, potTopY + rimHeight)
            lineTo(centerX + (potTopWidth / 2f) - 6f, potTopY + rimHeight)
            lineTo(centerX + (potBottomWidth / 2f), potBottomY)
            lineTo(centerX, potBottomY)
            close()
        }
        drawPath(
            path = potShadowPath,
            color = Color(0xFFC05A3E).copy(alpha = 0.4f)
        )

        // Pot Rim / Lip
        drawRoundRect(
            color = Color(0xFFE07A5F),
            topLeft = Offset(centerX - (potTopWidth / 2f), potTopY),
            size = Size(potTopWidth, rimHeight),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Cute Face on Pot
        val eyeY = potTopY + rimHeight + 16f
        drawCircle(color = Slate900, radius = 3.5f, center = Offset(centerX - 16f, eyeY))
        drawCircle(color = Slate900, radius = 3.5f, center = Offset(centerX + 16f, eyeY))
        val smilePath = Path().apply {
            moveTo(centerX - 8f, eyeY + 8f)
            quadraticTo(
                centerX, eyeY + 16f,
                centerX + 8f, eyeY + 8f
            )
        }
        drawPath(
            path = smilePath,
            color = Slate900,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // --- Helper Drawing Functions ---
        fun drawLeaf(center: Offset, scaleX: Float, scaleY: Float) {
            val leafPath = Path().apply {
                moveTo(center.x, center.y - 18f * scaleY)
                quadraticTo(
                    center.x + 18f * scaleX, center.y,
                    center.x, center.y + 18f * scaleY
                )
                quadraticTo(
                    center.x - 18f * scaleX, center.y,
                    center.x, center.y - 18f * scaleY
                )
                close()
            }
            drawPath(path = leafPath, color = Emerald500)
            // Leaf vein
            drawLine(
                color = Emerald700,
                start = Offset(center.x, center.y - 14f * scaleY),
                end = Offset(center.x, center.y + 14f * scaleY),
                strokeWidth = 2.5f
            )
        }

        fun drawYellowFlower(center: Offset, radius: Float) {
            // 5-petal yellow star flower
            val flowerPath = Path()
            val petals = 5
            for (i in 0 until petals * 2) {
                val r = if (i % 2 == 0) radius else radius * 0.45f
                val angle = Math.toRadians((i * 360 / (petals * 2) - 90).toDouble())
                val x = center.x + r * cos(angle).toFloat()
                val y = center.y + r * sin(angle).toFloat()
                if (i == 0) flowerPath.moveTo(x, y) else flowerPath.lineTo(x, y)
            }
            flowerPath.close()
            drawPath(path = flowerPath, color = Color(0xFFFFD54F))
            // Center yellow-orange blossom core
            drawCircle(color = Color(0xFFF57C00), radius = radius * 0.35f, center = center)
        }

        fun drawTomato(center: Offset, radius: Float) {
            // Tomato body
            drawCircle(
                color = Color(0xFFE53935),
                radius = radius,
                center = center
            )
            // White highlight reflection
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = radius * 0.25f,
                center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f)
            )
            // Green Cap / Sepals
            val sepalPath = Path().apply {
                moveTo(center.x, center.y - radius - 2f)
                lineTo(center.x - 5f, center.y - radius + 3f)
                lineTo(center.x + 5f, center.y - radius + 3f)
                close()
            }
            drawPath(path = sepalPath, color = Emerald600)
        }

        // --- 2. Stage-Specific Plant Growth Canvas Illustrations ---
        when (growthStage) {
            PlantGrowthStage.SEEDLING_SPROUT -> {
                // STAGE 0: Small green sprout emerging from soil with tiny two leaves
                val sproutStemPath = Path().apply {
                    moveTo(centerX, potTopY)
                    quadraticTo(
                        centerX - 4f, height * 0.58f,
                        centerX, height * 0.48f
                    )
                }
                drawPath(
                    path = sproutStemPath,
                    color = Emerald600,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )

                // Left tiny sprout leaf
                val leftSproutLeaf = Path().apply {
                    moveTo(centerX, height * 0.48f)
                    quadraticTo(
                        centerX - 22f, height * 0.40f,
                        centerX - 28f, height * 0.45f
                    )
                    quadraticTo(
                        centerX - 16f, height * 0.52f,
                        centerX, height * 0.48f
                    )
                    close()
                }
                drawPath(path = leftSproutLeaf, color = Emerald500)

                // Right tiny sprout leaf
                val rightSproutLeaf = Path().apply {
                    moveTo(centerX, height * 0.48f)
                    quadraticTo(
                        centerX + 22f, height * 0.40f,
                        centerX + 28f, height * 0.45f
                    )
                    quadraticTo(
                        centerX + 16f, height * 0.52f,
                        centerX, height * 0.48f
                    )
                    close()
                }
                drawPath(path = rightSproutLeaf, color = Emerald500)

                // Tiny inner leaf highlights
                drawCircle(color = Emerald200, radius = 2.5f, center = Offset(centerX - 14f, height * 0.45f))
                drawCircle(color = Emerald200, radius = 2.5f, center = Offset(centerX + 14f, height * 0.45f))

                // Sparkle
                drawCircle(color = Amber500, radius = 2.5f, center = Offset(centerX, height * 0.38f))
            }

            PlantGrowthStage.VEGETATIVE_LEAFY -> {
                // STAGE 1: Medium leafy stem with vibrant green branches
                val mainStemPath = Path().apply {
                    moveTo(centerX, potTopY)
                    cubicTo(
                        centerX - 12f, height * 0.52f,
                        centerX + 10f, height * 0.38f,
                        centerX - 2f, height * 0.25f
                    )
                }
                drawPath(
                    path = mainStemPath,
                    color = Emerald600,
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )

                // Left Branch
                val leftBranch = Path().apply {
                    moveTo(centerX - 5f, height * 0.50f)
                    quadraticTo(centerX - 28f, height * 0.44f, centerX - 38f, height * 0.38f)
                }
                drawPath(path = leftBranch, color = Emerald600, style = Stroke(width = 6f, cap = StrokeCap.Round))

                // Right Branch
                val rightBranch = Path().apply {
                    moveTo(centerX + 4f, height * 0.40f)
                    quadraticTo(centerX + 28f, height * 0.34f, centerX + 38f, height * 0.28f)
                }
                drawPath(path = rightBranch, color = Emerald600, style = Stroke(width = 6f, cap = StrokeCap.Round))

                // Leaves
                drawLeaf(center = Offset(centerX - 2f, height * 0.22f), scaleX = 0.9f, scaleY = 1.0f)
                drawLeaf(center = Offset(centerX - 40f, height * 0.36f), scaleX = 1.0f, scaleY = 0.85f)
                drawLeaf(center = Offset(centerX - 22f, height * 0.32f), scaleX = 0.8f, scaleY = 0.8f)
                drawLeaf(center = Offset(centerX + 40f, height * 0.26f), scaleX = 1.0f, scaleY = 0.85f)
                drawLeaf(center = Offset(centerX + 22f, height * 0.22f), scaleX = 0.8f, scaleY = 0.8f)

                // Sparkles
                drawCircle(color = Amber500, radius = 3f, center = Offset(centerX - 42f, height * 0.22f))
                drawCircle(color = Amber500, radius = 2f, center = Offset(centerX + 42f, height * 0.16f))
            }

            PlantGrowthStage.FLOWERING -> {
                // STAGE 2: Taller plant with yellow tomato blossom flowers
                val mainStemPath = Path().apply {
                    moveTo(centerX, potTopY)
                    cubicTo(
                        centerX - 16f, height * 0.50f,
                        centerX + 12f, height * 0.32f,
                        centerX - 4f, height * 0.18f
                    )
                }
                drawPath(
                    path = mainStemPath,
                    color = Emerald600,
                    style = Stroke(width = 9f, cap = StrokeCap.Round)
                )

                // Branches
                val leftBranch = Path().apply {
                    moveTo(centerX - 6f, height * 0.48f)
                    quadraticTo(centerX - 30f, height * 0.40f, centerX - 42f, height * 0.32f)
                }
                drawPath(path = leftBranch, color = Emerald600, style = Stroke(width = 6f, cap = StrokeCap.Round))

                val rightBranch = Path().apply {
                    moveTo(centerX + 5f, height * 0.36f)
                    quadraticTo(centerX + 30f, height * 0.30f, centerX + 42f, height * 0.22f)
                }
                drawPath(path = rightBranch, color = Emerald600, style = Stroke(width = 6f, cap = StrokeCap.Round))

                // Leaves
                drawLeaf(center = Offset(centerX - 4f, height * 0.15f), scaleX = 0.9f, scaleY = 1.0f)
                drawLeaf(center = Offset(centerX - 44f, height * 0.30f), scaleX = 1.0f, scaleY = 0.85f)
                drawLeaf(center = Offset(centerX - 24f, height * 0.26f), scaleX = 0.8f, scaleY = 0.8f)
                drawLeaf(center = Offset(centerX + 44f, height * 0.20f), scaleX = 1.0f, scaleY = 0.85f)
                drawLeaf(center = Offset(centerX + 24f, height * 0.16f), scaleX = 0.8f, scaleY = 0.8f)

                // Yellow Blossom Flowers
                drawYellowFlower(center = Offset(centerX - 30f, height * 0.42f), radius = 11f)
                drawYellowFlower(center = Offset(centerX + 28f, height * 0.32f), radius = 12f)
                drawYellowFlower(center = Offset(centerX + 6f, height * 0.22f), radius = 10f)

                // Sparkles
                drawCircle(color = Amber500, radius = 3f, center = Offset(centerX - 48f, height * 0.16f))
                drawCircle(color = Amber500, radius = 2.5f, center = Offset(centerX + 48f, height * 0.12f))
            }

            PlantGrowthStage.FRUITING_HARVEST -> {
                // STAGE 3+: Full lush plant with bright red ripe tomatoes and green stems
                val mainStemPath = Path().apply {
                    moveTo(centerX, potTopY)
                    cubicTo(
                        centerX - 20f, height * 0.50f,
                        centerX + 15f, height * 0.35f,
                        centerX - 5f, height * 0.15f
                    )
                }
                drawPath(
                    path = mainStemPath,
                    color = Emerald600,
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )

                // Branch Left
                val leftBranchPath = Path().apply {
                    moveTo(centerX - 8f, height * 0.48f)
                    quadraticTo(centerX - 35f, height * 0.42f, centerX - 48f, height * 0.35f)
                }
                drawPath(path = leftBranchPath, color = Emerald600, style = Stroke(width = 7f, cap = StrokeCap.Round))

                // Branch Right
                val rightBranchPath = Path().apply {
                    moveTo(centerX + 6f, height * 0.38f)
                    quadraticTo(centerX + 35f, height * 0.32f, centerX + 48f, height * 0.25f)
                }
                drawPath(path = rightBranchPath, color = Emerald600, style = Stroke(width = 7f, cap = StrokeCap.Round))

                // Foliage / Leaves
                drawLeaf(center = Offset(centerX - 5f, height * 0.12f), scaleX = 1.0f, scaleY = 1.1f)
                drawLeaf(center = Offset(centerX - 50f, height * 0.33f), scaleX = 1.1f, scaleY = 0.9f)
                drawLeaf(center = Offset(centerX - 28f, height * 0.28f), scaleX = 0.9f, scaleY = 0.9f)
                drawLeaf(center = Offset(centerX + 50f, height * 0.23f), scaleX = 1.1f, scaleY = 0.9f)
                drawLeaf(center = Offset(centerX + 28f, height * 0.18f), scaleX = 0.9f, scaleY = 0.9f)

                // Ripe Tomatoes
                drawTomato(center = Offset(centerX - 35f, height * 0.45f), radius = 13f)
                drawTomato(center = Offset(centerX + 32f, height * 0.35f), radius = 15f)
                drawTomato(center = Offset(centerX + 8f, height * 0.25f), radius = 12f)

                // Sparkles
                drawCircle(color = Amber500, radius = 3f, center = Offset(centerX - 55f, height * 0.18f))
                drawCircle(color = Amber500, radius = 2f, center = Offset(centerX + 58f, height * 0.12f))
                drawCircle(color = Amber500, radius = 2.5f, center = Offset(centerX + 20f, height * 0.08f))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
fun PlantAvatarCanvasStagesPreview() {
    AgriQuestTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlantAvatarCanvas(growthStage = PlantGrowthStage.SEEDLING_SPROUT, size = 90.dp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlantAvatarCanvas(growthStage = PlantGrowthStage.VEGETATIVE_LEAFY, size = 90.dp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlantAvatarCanvas(growthStage = PlantGrowthStage.FLOWERING, size = 90.dp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlantAvatarCanvas(growthStage = PlantGrowthStage.FRUITING_HARVEST, size = 90.dp)
            }
        }
    }
}
