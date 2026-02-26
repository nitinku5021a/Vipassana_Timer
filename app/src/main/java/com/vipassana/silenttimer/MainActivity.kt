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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vipassana.silenttimer.ui.AwarenessRunningScreen
import com.vipassana.silenttimer.ui.AwarenessSetupScreen
import com.vipassana.silenttimer.ui.CalendarLogScreen
import com.vipassana.silenttimer.ui.CompletionScreen
import com.vipassana.silenttimer.ui.HomeScreen
import com.vipassana.silenttimer.ui.TimerScreen
import com.vipassana.silenttimer.ui.theme.VipassanaTheme
import kotlinx.coroutines.launch

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
@OptIn(ExperimentalMaterial3Api::class)
fun VipassanaApp(viewModel: TimerViewModel) {
    val isRunning by viewModel.isRunning.collectAsState()
    val isInPrep by viewModel.isInPrep.collectAsState()
    val hasCompleted by viewModel.hasCompleted.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val totalDuration by viewModel.totalDuration.collectAsState()
    val dailyLogs by viewModel.dailyLogs.collectAsState()
    val completionTitle by viewModel.completionTitle.collectAsState()
    val completionDuration by viewModel.completionDuration.collectAsState()
    val isAwarenessRunning by viewModel.isAwarenessRunning.collectAsState()
    val awarenessTimeLeft by viewModel.awarenessTimeLeft.collectAsState()
    val awarenessTotalDuration by viewModel.awarenessTotalDuration.collectAsState()
    val awarenessInterval by viewModel.awarenessInterval.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showLog by remember { mutableStateOf(false) }
    var showAwareness by remember { mutableStateOf(false) }

    if (isRunning) {
        TimerScreen(
            timeLeft = timeLeft,
            totalDuration = totalDuration,
            isInPrep = isInPrep,
            onStop = { viewModel.stopTimer(context) }
        )
    } else if (isAwarenessRunning) {
        AwarenessRunningScreen(
            timeLeft = awarenessTimeLeft,
            totalDuration = awarenessTotalDuration,
            intervalMillis = awarenessInterval,
            onStop = { viewModel.stopAwareness(context) }
        )
    } else {
        if (hasCompleted) {
            CompletionScreen(
                title = completionTitle,
                totalDuration = completionDuration,
                onDone = {
                    viewModel.clearCompletion()
                    showLog = false
                    showAwareness = false
                }
            )
        } else {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Text(
                            text = "Vipassana",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                        NavigationDrawerItem(
                            label = { Text("Home") },
                            selected = !showLog && !showAwareness,
                            onClick = {
                                showLog = false
                                showAwareness = false
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Meditation Log") },
                            selected = showLog,
                            onClick = {
                                showLog = true
                                showAwareness = false
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Be Aware Always") },
                            selected = showAwareness,
                            onClick = {
                                showAwareness = true
                                showLog = false
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    when {
                                        showLog -> "Meditation Log"
                                        showAwareness -> "Be Aware Always"
                                        else -> "Vipassana"
                                    }
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    if (showLog) {
                        LaunchedEffect(Unit) {
                            viewModel.refreshLogs(context)
                        }
                        CalendarLogScreen(
                            logs = dailyLogs,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else if (showAwareness) {
                        AwarenessSetupScreen(
                            onStart = { hours, intervalMinutes ->
                                val durationMillis = hours.toLong() * 60 * 60 * 1000
                                val intervalMillis = intervalMinutes.toLong() * 60 * 1000
                                viewModel.startAwareness(context, durationMillis, intervalMillis)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        HomeScreen(
                            onDurationSelected = { duration ->
                                viewModel.startTimer(context, duration)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
