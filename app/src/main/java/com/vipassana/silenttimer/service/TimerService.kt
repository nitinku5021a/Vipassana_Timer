package com.vipassana.silenttimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.vipassana.silenttimer.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerService : Service() {

    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    private var timer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null

    private val _timeLeftInMillis = MutableStateFlow(0L)
    val timeLeftInMillis: StateFlow<Long> = _timeLeftInMillis.asStateFlow()

    private val _totalDurationInMillis = MutableStateFlow(0L)
    val totalDurationInMillis: StateFlow<Long> = _totalDurationInMillis.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    companion object {
        const val CHANNEL_ID = "VipassanaTimerChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.vipassana.silenttimer.START"
        const val ACTION_STOP = "com.vipassana.silenttimer.STOP"
        const val EXTRA_DURATION = "com.vipassana.silenttimer.DURATION"
    }

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
                if (duration > 0) {
                    startTimerInternal(duration)
                }
            }
            ACTION_STOP -> stopTimer()
        }
        return START_NOT_STICKY
    }

    private fun startTimerInternal(durationMillis: Long) {
        if (_isTimerRunning.value) return

        // Acquire WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Vipassana::TimerWakeLock")
        wakeLock?.acquire(durationMillis + 10000)

        // Start Foreground IMMEDIATELY
        startForeground(NOTIFICATION_ID, createNotification("Consulting the silence..."))

        // Play Start Gong
        playSound(R.raw.gong_start)

        _isTimerRunning.value = true
        _totalDurationInMillis.value = durationMillis
        timer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeftInMillis.value = millisUntilFinished
                updateNotification("Time remaining: ${formatTime(millisUntilFinished)}")
            }

            override fun onFinish() {
                _timeLeftInMillis.value = 0
                playEndGongs()
                stopForeground(STOP_FOREGROUND_REMOVE)
                _isTimerRunning.value = false
                releaseWakeLock()
            }
        }.start()
    }

    // Exposed for Binder if needed, but primary start is via Intent now for safety
    fun startTimerProxy(durationMillis: Long) {
       // logic moved to startTimerInternal, this module can call it or deprecated
    }

    fun stopTimer() {
        timer?.cancel()
        _isTimerRunning.value = false
        _timeLeftInMillis.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        releaseWakeLock()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun playSound(resId: Int, loopCount: Int = 0) {
        try {
            // Request Audio Focus to ensure clarity
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.requestAudioFocus(
                null,
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, resId)
            
            if (mediaPlayer == null) {
                println("Vipassana: MediaPlayer creation failed for resId $resId")
                return
            }

            mediaPlayer?.apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setVolume(1.0f, 1.0f) // Max volume relative to stream
                setOnCompletionListener { 
                    // Completion logic
                }
                start()
                println("Vipassana: MediaPlayer started successfully")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Vipassana: Error playing sound: ${e.message}")
        }
    }

    private fun playEndGongs() {
        // Play gong 3 times with delay
        // Using a coroutine or simple thread for delay is risky in service if killing, 
        // but safe enough here for simple logic.
        // Better: Use a separate logic. For simplicity, let's play one long sound or 3 rapid ones.
        // If the user provides a single "3 gongs" file, it's easier.
        // Assuming individual gong:
        
        Thread {
            for (i in 1..3) {
                 playSound(R.raw.gong_end)
                 try { Thread.sleep(3000) } catch (e: InterruptedException) {}
            }
        }.start()
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun createNotificationChannel() {
        val name = "Vipassana Timer"
        val descriptionText = "Shows active meditation timer"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, com.vipassana.silenttimer.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vipassana Meditation")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(content))
    }
    
    private fun formatTime(millis: Long): String {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }
}
