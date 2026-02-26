package com.vipassana.silenttimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AwarenessSetupScreen(
    onStart: (totalHours: Int, intervalMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var hoursText by remember { mutableStateOf("8") }
    var intervalText by remember { mutableStateOf("10") }
    var errorText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Be Aware Always",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Set total hours",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextField(
            value = hoursText,
            onValueChange = { if (it.all { ch -> ch.isDigit() }) hoursText = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Gong interval (minutes)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextField(
            value = intervalText,
            onValueChange = { if (it.all { ch -> ch.isDigit() }) intervalText = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(top = 8.dp)
        )

        if (errorText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                .clickable {
                    val hours = hoursText.toIntOrNull()
                    val interval = intervalText.toIntOrNull()
                    if (hours == null || hours <= 0) {
                        errorText = "Please enter hours greater than 0."
                        return@clickable
                    }
                    if (interval == null || interval <= 0) {
                        errorText = "Please enter interval minutes greater than 0."
                        return@clickable
                    }
                    errorText = ""
                    onStart(hours, interval)
                }
                .padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Start Awareness",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun AwarenessRunningScreen(
    timeLeft: Long,
    totalDuration: Long,
    intervalMillis: Long,
    onStop: () -> Unit
) {
    val minutes = (timeLeft / 1000) / 60
    val seconds = (timeLeft / 1000) % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val intervalMinutes = (intervalMillis / 1000) / 60
    val totalHours = totalDuration / (1000 * 60 * 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Be Aware Always",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Total ${totalHours}h - Gong every ${intervalMinutes}m",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = timeFormatted,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                .clickable(onClick = onStop)
                .padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                text = "End Awareness",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
