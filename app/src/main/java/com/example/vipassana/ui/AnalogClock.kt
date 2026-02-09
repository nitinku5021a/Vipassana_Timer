package com.example.vipassana.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogClock(
    timeLeft: Long,
    totalDuration: Long
) {
    val progress = if (totalDuration > 0) timeLeft.toFloat() / totalDuration.toFloat() else 0f
    // progress goes from 1.0 -> 0.0
    // We want the arc to reduce.
    // Full circle is 360.
    val sweepAngle = 360f * progress
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        val strokeWidth = 12.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(
            (size.width - diameter) / 2,
            (size.height - diameter) / 2
        )
        val size = Size(diameter, diameter)

        // Draw background track
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )

        // Draw progress arc
        // Start from -90 (top) and go clockwise?
        // Usually timer reduces clockwise. 
        // Let's start at -90 and sweep 'sweepAngle'.
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = - (360f - sweepAngle), // Reduces counter-clockwise or clockwise?
            // If we want it to look like time is running OUT:
            // Full circle, then shrinks. 
            // Better: Start -90. Sweep = 360 * progress.
            // As progress 1->0, sweep 360->0.
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Optional: Draw a "hand" or a dot at the tip
        // Math for dot position
        // angle in radians. Start -90deg is -PI/2.
        // current angle = -90 + (360 * progress) ?? No, that would move it 
        // Let's stick to simple arc for "Vipassana" minimalism.
    }
}
