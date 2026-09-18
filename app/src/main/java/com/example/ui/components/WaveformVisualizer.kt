package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    isListening: Boolean,
    audioRms: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 9,
    width: Dp = 110.dp,
    height: Dp = 36.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val gradientColors = listOf(
        Color(0xFF00D2FF),
        Color(0xFF3A7BD5),
        Color(0xFF9D50BB)
    )

    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalSpacing = canvasWidth * 0.35f
        val barWidth = (canvasWidth - totalSpacing) / barCount
        val spacing = totalSpacing / (barCount - 1).coerceAtLeast(1)

        val brush = Brush.verticalGradient(
            colors = if (isListening) gradientColors else listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.2f))
        )

        val baseNormRms = (audioRms / 10f).coerceIn(0.1f, 1.0f)

        for (i in 0 until barCount) {
            val offsetFraction = i.toFloat() / barCount
            val sineWave = if (isListening) {
                ((sin(phase + offsetFraction * 4.0) + 1.0) / 2.0).toFloat()
            } else {
                0.15f
            }

            // Height calculation with minimum threshold
            val calculatedHeight = if (isListening) {
                val amplitude = (baseNormRms * 0.7f + sineWave * 0.3f)
                (canvasHeight * 0.2f + canvasHeight * 0.75f * amplitude).coerceIn(canvasHeight * 0.18f, canvasHeight * 0.95f)
            } else {
                canvasHeight * 0.15f
            }

            val x = i * (barWidth + spacing)
            val y = (canvasHeight - calculatedHeight) / 2f

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, calculatedHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
