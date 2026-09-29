package com.echo.recorder.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.PrimaryBlue
import kotlin.random.Random

/**
 * Standard Audio Editor Fluent Solid-Filled Cloudy Waveform Visualizer.
 * Creates a continuous, smooth Bezier envelope filled solidly (mirrored positive & negative peaks),
 * matching professional DAW/audio-editing tools (Audacity, Audition, SoundForge).
 */
@Composable
fun AudioWaveformVisualizer(
    amplitude: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    sampleCount: Int = 48
) {
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isRecording) amplitude.coerceIn(0.04f, 1.0f) else 0.03f,
        animationSpec = tween(durationMillis = 80),
        label = "waveformAmplitude"
    )

    // Running buffer of recent audio peaks for fluent continuous scrolling
    val samples = remember {
        mutableStateListOf<Float>().apply {
            repeat(sampleCount) { add(0.05f) }
        }
    }

    LaunchedEffect(animatedAmplitude, isRecording) {
        if (samples.size >= sampleCount) {
            samples.removeAt(0)
        }
        val targetVal = if (isRecording) {
            // Apply gentle smoothing with slight natural acoustic micro-variation
            val variance = 0.85f + Random.nextFloat() * 0.30f
            (animatedAmplitude * variance).coerceIn(0.05f, 1.0f)
        } else {
            0.04f
        }
        samples.add(targetVal)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val maxPeakHeight = (height * 0.46f)

        if (width <= 0f || height <= 0f || samples.isEmpty()) return@Canvas

        val stepX = width / (samples.size - 1).coerceAtLeast(1)

        // Precompute upper and lower envelope points
        val upperPoints = mutableListOf<Offset>()
        val lowerPoints = mutableListOf<Offset>()

        for (i in samples.indices) {
            val x = i * stepX
            val amp = samples[i]
            val peakH = (amp * maxPeakHeight).coerceAtLeast(2.5f)
            upperPoints.add(Offset(x, centerY - peakH))
            lowerPoints.add(Offset(x, centerY + peakH))
        }

        // Helper to construct a smooth cubic Bezier path along points
        fun buildSmoothEnvelope(points: List<Offset>, returnPoints: List<Offset>): Path {
            val path = Path()
            if (points.isEmpty()) return path

            path.moveTo(points.first().x, points.first().y)

            // Smooth cubic Bezier along top envelope
            for (i in 0 until points.size - 1) {
                val p0 = points[if (i > 0) i - 1 else i]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = points[if (i + 2 < points.size) i + 2 else i + 1]

                val cp1x = p1.x + (p2.x - p0.x) / 6f
                val cp1y = p1.y + (p2.y - p0.y) / 6f
                val cp2x = p2.x - (p3.x - p1.x) / 6f
                val cp2y = p2.y - (p3.y - p1.y) / 6f

                path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
            }

            // Connect to bottom envelope and return smoothly backwards
            path.lineTo(returnPoints.last().x, returnPoints.last().y)

            val rev = returnPoints.asReversed()
            for (i in 0 until rev.size - 1) {
                val p0 = rev[if (i > 0) i - 1 else i]
                val p1 = rev[i]
                val p2 = rev[i + 1]
                val p3 = rev[if (i + 2 < rev.size) i + 2 else i + 1]

                val cp1x = p1.x + (p2.x - p0.x) / 6f
                val cp1y = p1.y + (p2.y - p0.y) / 6f
                val cp2x = p2.x - (p3.x - p1.x) / 6f
                val cp2y = p2.y - (p3.y - p1.y) / 6f

                path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
            }

            path.close()
            return path
        }

        val cloudPath = buildSmoothEnvelope(upperPoints, lowerPoints)

        // 1. Soft Ambient Cloudy Glow Layer (Diffuse outer volume)
        drawPath(
            path = cloudPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    PrimaryBlue.copy(alpha = if (isRecording) 0.35f else 0.15f),
                    AccentPurple.copy(alpha = if (isRecording) 0.45f else 0.18f),
                    PrimaryBlue.copy(alpha = if (isRecording) 0.35f else 0.15f)
                ),
                startY = 0f,
                endY = height
            )
        )

        // 2. Solid Filled Cloudy Audio Waveform Core (Sharp, rich DAW standard shape)
        drawPath(
            path = cloudPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    PrimaryBlue.copy(alpha = if (isRecording) 0.90f else 0.40f),
                    Color(0xFF38BDF8).copy(alpha = if (isRecording) 0.95f else 0.45f),
                    AccentPurple.copy(alpha = if (isRecording) 0.90f else 0.40f)
                ),
                startY = height * 0.15f,
                endY = height * 0.85f
            )
        )

        // 3. Crisp Contour Edge Line
        drawPath(
            path = cloudPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    PrimaryBlue.copy(alpha = 0.5f),
                    Color.White.copy(alpha = if (isRecording) 0.85f else 0.4f),
                    AccentPurple.copy(alpha = 0.5f)
                )
            ),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // 4. Subtle Center Axis Reference Line
        drawLine(
            color = Color.White.copy(alpha = if (isRecording) 0.22f else 0.10f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1.dp.toPx()
        )
    }
}
