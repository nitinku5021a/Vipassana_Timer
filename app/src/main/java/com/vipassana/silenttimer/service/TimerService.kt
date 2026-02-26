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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.vipassana.silenttimer.R
import com.vipassana.silenttimer.logging.MeditationLogStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerService : Service() {

    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    private var timer: CountDownTimer? = null
    private var prepTimer: CountDownTimer? = null
    private var awarenessTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null
    private var hasPlayedPreEndDong = false
    private var hasLoggedSession = false

    private val _timeLeftInMillis = MutableStateFlow(0L)
    val timeLeftInMillis: StateFlow<Long> = _timeLeftInMillis.asStateFlow()

    private val _totalDurationInMillis = MutableStateFlow(0L)
    val totalDurationInMillis: StateFlow<Long> = _totalDurationInMillis.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _isInPrep = MutableStateFlow(false)
    val isInPrep: StateFlow<Boolean> = _isInPrep.asStateFlow()

    private val _hasCompleted = MutableStateFlow(false)
    val hasCompleted: StateFlow<Boolean> = _hasCompleted.asStateFlow()

    private val _completionTitle = MutableStateFlow("Session Complete")
    val completionTitle: StateFlow<String> = _completionTitle.asStateFlow()

    private val _completionDuration = MutableStateFlow(0L)
    val completionDuration: StateFlow<Long> = _completionDuration.asStateFlow()

    private val _isAwarenessRunning = MutableStateFlow(false)
    val isAwarenessRunning: StateFlow<Boolean> = _isAwarenessRunning.asStateFlow()

    private val _awarenessTimeLeftInMillis = MutableStateFlow(0L)
    val awarenessTimeLeftInMillis: StateFlow<Long> = _awarenessTimeLeftInMillis.asStateFlow()

    private val _awarenessTotalDurationInMillis = MutableStateFlow(0L)
    val awarenessTotalDurationInMillis: StateFlow<Long> = _awarenessTotalDurationInMillis.asStateFlow()

    private val _awarenessIntervalInMillis = MutableStateFlow(0L)
    val awarenessIntervalInMillis: StateFlow<Long> = _awarenessIntervalInMillis.asStateFlow()

    companion object {
        const val CHANNEL_ID = "VipassanaTimerChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.vipassana.silenttimer.START"
        const val ACTION_STOP = "com.vipassana.silenttimer.STOP"
        const val EXTRA_DURATION = "com.vipassana.silenttimer.DURATION"
        const val ACTION_START_AWARENESS = "com.vipassana.silenttimer.START_AWARENESS"
        const val ACTION_STOP_AWARENESS = "com.vipassana.silenttimer.STOP_AWARENESS"
        const val EXTRA_AWARENESS_DURATION = "com.vipassana.silenttimer.AWARENESS_DURATION"
        const val EXTRA_AWARENESS_INTERVAL = "com.vipassana.silenttimer.AWARENESS_INTERVAL"
        private const val PREP_TIME_MILLIS = 8 * 1000L
        private const val PRE_END_DONG_OFFSET_MILLIS = 5 * 60 * 1000L
        private const val MIN_DURATION_FOR_PRE_DONG_MILLIS = 30 * 60 * 1000L
        private const val GONG_PLAY_DURATION_MILLIS = 9 * 1000L
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
            ACTION_START_AWARENESS -> {
                val duration = intent.getLongExtra(EXTRA_AWARENESS_DURATION, 0L)
                val interval = intent.getLongExtra(EXTRA_AWARENESS_INTERVAL, 0L)
                if (duration > 0 && interval > 0) {
                    startAwarenessInternal(duration, interval)
                }
            }
            ACTION_STOP_AWARENESS -> stopAwareness()
        }
        return START_NOT_STICKY
    }

    private fun startTimerInternal(durationMillis: Long) {
        if (_isTimerRunning.value) return
        hasPlayedPreEndDong = false
        hasLoggedSession = false
        _hasCompleted.value = false
        _completionTitle.value = "Session Complete"
        _completionDuration.value = 0L
        _isInPrep.value = true

        // Acquire WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Vipassana::TimerWakeLock")
        wakeLock?.acquire(durationMillis + 10000)

        // Start Foreground IMMEDIATELY
        startForeground(NOTIFICATION_ID, createNotification("Consulting the silence..."))

        _isTimerRunning.value = true
        _totalDurationInMillis.value = durationMillis
        prepTimer = object : CountDownTimer(PREP_TIME_MILLIS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeftInMillis.value = millisUntilFinished
                updateNotification("Starting in: ${formatTime(millisUntilFinished)}")
            }

            override fun onFinish() {
                _isInPrep.value = false
                // Play Start Gong at actual start
                playSound(R.raw.gong_start)
                timer = object : CountDownTimer(durationMillis, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        _timeLeftInMillis.value = millisUntilFinished
                        updateNotification("Time remaining: ${formatTime(millisUntilFinished)}")
                        if (!hasPlayedPreEndDong &&
                            durationMillis > MIN_DURATION_FOR_PRE_DONG_MILLIS &&
                            millisUntilFinished <= PRE_END_DONG_OFFSET_MILLIS
                        ) {
                            playSound(R.raw.gong_start)
                            hasPlayedPreEndDong = true
                        }
                    }

                    override fun onFinish() {
                        _timeLeftInMillis.value = 0
                        logSession(durationMillis)
                        hasLoggedSession = true
                        _completionTitle.value = "Session Complete"
                        _completionDuration.value = durationMillis
                        _hasCompleted.value = true
                        playEndGongs()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        _isTimerRunning.value = false
                        _isInPrep.value = false
                        releaseWakeLock()
                    }
                }.start()
            }
        }.start()
    }

    // Exposed for Binder if needed, but primary start is via Intent now for safety
    fun startTimerProxy(durationMillis: Long) {
       // logic moved to startTimerInternal, this module can call it or deprecated
    }

    fun clearCompletion() {
        _hasCompleted.value = false
        _completionTitle.value = "Session Complete"
        _completionDuration.value = 0L
    }

    fun stopTimer() {
        val elapsed = _totalDurationInMillis.value - _timeLeftInMillis.value
        if (!hasLoggedSession && elapsed > 0) {
            logSession(elapsed)
            hasLoggedSession = true
        }
        prepTimer?.cancel()
        timer?.cancel()
        _isTimerRunning.value = false
        _isInPrep.value = false
        _hasCompleted.value = false
        _completionTitle.value = "Session Complete"
        _completionDuration.value = 0L
        _timeLeftInMillis.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        releaseWakeLock()
        stopRunnable?.let { mainHandler.removeCallbacks(it) }
        stopRunnable = null
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startAwarenessInternal(durationMillis: Long, intervalMillis: Long) {
        if (_isAwarenessRunning.value || _isTimerRunning.value) return
        _hasCompleted.value = false
        _completionTitle.value = "Awareness Complete"
        _completionDuration.value = 0L
        _isAwarenessRunning.value = true
        _awarenessTotalDurationInMillis.value = durationMillis
        _awarenessIntervalInMillis.value = intervalMillis
        _awarenessTimeLeftInMillis.value = durationMillis

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Vipassana::AwarenessWakeLock")
        wakeLock?.acquire(durationMillis + 10000)

        startForeground(NOTIFICATION_ID, createNotification("Awareness in progress..."))

        var nextGongAtMillis = intervalMillis
        awarenessTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _awarenessTimeLeftInMillis.value = millisUntilFinished
                updateNotification("Awareness: ${formatTime(millisUntilFinished)} remaining")
                val elapsed = durationMillis - millisUntilFinished
                if (nextGongAtMillis < durationMillis && elapsed >= nextGongAtMillis) {
                    playSound(R.raw.gong_start)
                    nextGongAtMillis += intervalMillis
                }
            }

            override fun onFinish() {
                _awarenessTimeLeftInMillis.value = 0
                _completionTitle.value = "Awareness Complete"
                _completionDuration.value = durationMillis
                _hasCompleted.value = true
                playEndGongs()
                stopForeground(STOP_FOREGROUND_REMOVE)
                _isAwarenessRunning.value = false
                releaseWakeLock()
            }
        }.start()
    }

    fun stopAwareness() {
        awarenessTimer?.cancel()
        _isAwarenessRunning.value = false
        _awarenessTimeLeftInMillis.value = 0
        _awarenessTotalDurationInMillis.value = 0
        _awarenessIntervalInMillis.value = 0
        _hasCompleted.value = false
        _completionTitle.value = "Session Complete"
        _completionDuration.value = 0L
        stopForeground(STOP_FOREGROUND_REMOVE)
        releaseWakeLock()
        stopRunnable?.let { mainHandler.removeCallbacks(it) }
        stopRunnable = null
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun playSound(resId: Int, durationMillis: Long = GONG_PLAY_DURATION_MILLIS) {
        try {
            // Request Audio Focus to ensure clarity
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.requestAudioFocus(
                null,
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )

            stopRunnable?.let { mainHandler.removeCallbacks(it) }
            stopRunnable = null
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
                setOnCompletionListener { player ->
                    player.release()
                    if (mediaPlayer === player) {
                        mediaPlayer = null
                    }
                }
                val stopTask = Runnable {
                    val player = mediaPlayer
                    if (player != null) {
                        try {
                            if (player.isPlaying) {
                                player.stop()
                            }
                        } catch (_: IllegalStateException) {
                        }
                        player.release()
                        if (mediaPlayer === player) {
                            mediaPlayer = null
                        }
                    }
                }
                stopRunnable = stopTask
                mainHandler.postDelayed(stopTask, durationMillis)
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
                 try { Thread.sleep(GONG_PLAY_DURATION_MILLIS) } catch (e: InterruptedException) {}
            }
        }.start()
    }

    private fun logSession(durationMillis: Long) {
        if (durationMillis <= 0) return
        MeditationLogStore.addSession(applicationContext, durationMillis)
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
        stopAwareness()
    }
}
