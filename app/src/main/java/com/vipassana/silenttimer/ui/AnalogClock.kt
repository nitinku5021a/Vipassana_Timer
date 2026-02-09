package com.vipassana.silenttimer.ui

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
        val strokeWidth = 5.dp.toPx() // Thinner stroke for elegance
        val diameter = size.minDimension - strokeWidth
        val radius = diameter / 2
        val center = Offset(size.width / 2, size.height / 2)
        val size = Size(diameter, diameter)
        val topLeft = Offset(
            (size.width - diameter) / 2,
            (size.height - diameter) / 2
        )

        // Draw subtle background track
        drawArc(
            color = trackColor.copy(alpha = 0.3f), // More subtle
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )

        // Draw progress arc
        // Start from -90. Sweep negative for clockwise reduction? 
        // Let's make it fill from 0 to 360 based on progress.
        // If progress is remaining time, 1.0 -> 0.0.
        // We want full circle at start, shrinking to 0.
        // Sweep = -360 * progress (Counter-clockwise shrink? No, clockwise shrink = start fixed, sweep reduces)
        // Fixed start at -90. Sweep = 360 * progress. 
        // As progress reduces, the end point moves back to -90.
        val currentSweep = 360f * progress
        
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = currentSweep, 
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Draw Handle (Dot at the tip)
        if (progress > 0) {
            val endAngleRad = Math.toRadians((-90 + currentSweep).toDouble())
            val handleCenter = Offset(
                x = center.x + (radius * cos(endAngleRad)).toFloat(),
                y = center.y + (radius * sin(endAngleRad)).toFloat()
            )
            
            drawCircle(
                color = primaryColor,
                radius = 12.dp.toPx(), // Slightly larger than stroke for a visible "knob"
                center = handleCenter
            )
            // Inner white dot for handle
             drawCircle(
                color = trackColor,
                radius = 6.dp.toPx(),
                center = handleCenter
            )
        }
    }
}
