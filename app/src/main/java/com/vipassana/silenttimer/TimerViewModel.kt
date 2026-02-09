package com.vipassana.silenttimer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vipassana.silenttimer.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {

    private var timerService: TimerService? = null
    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    private val _timeLeft = MutableStateFlow(0L)
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _totalDuration = MutableStateFlow(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            _isServiceBound.value = true
            
            // Observe service state
            viewModelScope.launch {
                timerService?.timeLeftInMillis?.collect { _timeLeft.value = it }
            }
            viewModelScope.launch {
                timerService?.totalDurationInMillis?.collect { _totalDuration.value = it }
            }
            viewModelScope.launch {
                timerService?.isTimerRunning?.collect { _isRunning.value = it }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            _isServiceBound.value = false
            timerService = null
        }
    }

    fun bindService(context: Context) {
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        if (_isServiceBound.value) {
            context.unbindService(connection)
            _isServiceBound.value = false
        }
    }

    fun startTimer(context: Context, durationMillis: Long) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_DURATION, durationMillis)
        }
        // Ensure service is started/promoted to foreground
        context.startForegroundService(intent)
    }

    fun stopTimer(context: Context) {
        timerService?.stopTimer()
        val intent = Intent(context, TimerService::class.java)
        intent.action = TimerService.ACTION_STOP
        context.startService(intent) // Send stop action
    }
}
