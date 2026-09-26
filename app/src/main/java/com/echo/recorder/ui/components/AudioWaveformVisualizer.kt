package com.echo.recorder.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.PrimaryBlue
import kotlin.random.Random

@Composable
fun AudioWaveformVisualizer(
    amplitude: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32
) {
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isRecording) amplitude.coerceIn(0.05f, 1.0f) else 0.05f,
        animationSpec = tween(durationMillis = 100),
        label = "amplitudeAnimation"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = (width / (barCount * 1.5f))
        val gap = barWidth * 0.5f

        for (i in 0 until barCount) {
            val randomFactor = if (isRecording) (0.4f + Random.nextFloat() * 0.6f) else 0.1f
            val barHeight = (height * animatedAmplitude * randomFactor).coerceAtLeast(6f)

            val x = i * (barWidth + gap) + gap
            val y = (height - barHeight) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryBlue, AccentPurple)
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
