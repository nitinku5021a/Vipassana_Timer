package com.vipassana.silenttimer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vipassana.silenttimer.ui.AwarenessRunningScreen
import com.vipassana.silenttimer.ui.AwarenessSetupScreen
import com.vipassana.silenttimer.ui.CalendarLogScreen
import com.vipassana.silenttimer.ui.CompletionScreen
import com.vipassana.silenttimer.ui.DrawerMenuItem
import com.vipassana.silenttimer.ui.GongSettingsScreen
import com.vipassana.silenttimer.ui.HomeScreen
import com.vipassana.silenttimer.billing.DonationViewModel
import com.vipassana.silenttimer.ui.SupportScreen
import com.vipassana.silenttimer.ui.TimerScreen
import com.vipassana.silenttimer.ui.theme.VipassanaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TimerViewModel by viewModels()
    private val donationViewModel: DonationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        setContent {
            VipassanaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    VipassanaApp(viewModel, donationViewModel)
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
fun VipassanaApp(viewModel: TimerViewModel, donationViewModel: DonationViewModel) {
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
    var showSupport by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    fun showHome() {
        showLog = false
        showAwareness = false
        showSupport = false
        showSettings = false
    }

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
                    showHome()
                }
            )
        } else {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.fillMaxHeight(),
                        drawerContainerColor = MaterialTheme.colorScheme.background
                    ) {
                        Text(
                            text = "Vipassana",
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 8.dp)
                        )
                        Text(
                            text = "SILENCE  ·  INSIGHT",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DrawerMenuItem(
                            title = "Sit",
                            icon = Icons.Outlined.SelfImprovement,
                            selected = !showLog && !showAwareness && !showSupport && !showSettings,
                            onClick = {
                                showHome()
                                scope.launch { drawerState.close() }
                            }
                        )
                        DrawerMenuItem(
                            title = "Meditation log",
                            icon = Icons.Outlined.CalendarMonth,
                            selected = showLog,
                            onClick = {
                                showLog = true
                                showAwareness = false
                                showSupport = false
                                showSettings = false
                                scope.launch { drawerState.close() }
                            }
                        )
                        DrawerMenuItem(
                            title = "Be aware always",
                            icon = Icons.Outlined.NotificationsNone,
                            selected = showAwareness,
                            onClick = {
                                showAwareness = true
                                showLog = false
                                showSupport = false
                                showSettings = false
                                scope.launch { drawerState.close() }
                            }
                        )
                        DrawerMenuItem(
                            title = "Gong sound",
                            icon = Icons.Outlined.VolumeUp,
                            selected = showSettings,
                            onClick = {
                                showSettings = true
                                showLog = false
                                showAwareness = false
                                showSupport = false
                                scope.launch { drawerState.close() }
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DrawerMenuItem(
                            title = "Donate",
                            icon = Icons.Outlined.FavoriteBorder,
                            selected = showSupport,
                            onClick = {
                                showSupport = true
                                showLog = false
                                showAwareness = false
                                showSettings = false
                                scope.launch { drawerState.close() }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    when {
                                        showLog -> "Meditation log"
                                        showAwareness -> "Be aware always"
                                        showSupport -> "Donate"
                                        showSettings -> "Gong sound"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                ) { innerPadding ->
                    if (showLog) {
                        LaunchedEffect(showLog, hasCompleted, completionDuration) {
                            viewModel.refreshLogs(context)
                        }
                        CalendarLogScreen(
                            logs = dailyLogs,
                            onDelete = { date -> viewModel.deleteLog(context, date) },
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
                    } else if (showSupport) {
                        SupportScreen(
                            donationViewModel = donationViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else if (showSettings) {
                        GongSettingsScreen(modifier = Modifier.padding(innerPadding))
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
