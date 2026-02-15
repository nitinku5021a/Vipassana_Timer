package com.vipassana.silenttimer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.vipassana.silenttimer.ui.CalendarLogScreen
import com.vipassana.silenttimer.ui.HomeScreen
import com.vipassana.silenttimer.ui.TimerScreen
import com.vipassana.silenttimer.ui.theme.VipassanaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VipassanaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    VipassanaApp(viewModel)
                }
            }
        }
        
        checkPermissions()
    }

    override fun onStart() {
        super.onStart()
        viewModel.bindService(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.unbindService(this)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}

@Composable
fun VipassanaApp(viewModel: TimerViewModel) {
    val isRunning by viewModel.isRunning.collectAsState()
    val isInPrep by viewModel.isInPrep.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val totalDuration by viewModel.totalDuration.collectAsState()
    val dailyLogs by viewModel.dailyLogs.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showLog by remember { mutableStateOf(false) }

    if (isRunning) {
        TimerScreen(
            timeLeft = timeLeft,
            totalDuration = totalDuration,
            isInPrep = isInPrep,
            onStop = { viewModel.stopTimer(context) }
        )
    } else {
        if (showLog) {
            LaunchedEffect(Unit) {
                viewModel.refreshLogs(context)
            }
            CalendarLogScreen(
                logs = dailyLogs,
                onBack = { showLog = false }
            )
        } else {
            HomeScreen(
                onDurationSelected = { duration ->
                    viewModel.startTimer(context, duration)
                },
                onOpenLog = { showLog = true }
            )
        }
    }
}
