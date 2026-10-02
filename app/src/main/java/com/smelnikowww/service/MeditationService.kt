package com.smelnikowww.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smelnikowww.audio.SoundPlayer
import com.smelnikowww.audio.SoundSource
import com.smelnikowww.data.ActiveSessionStore
import com.smelnikowww.domain.ChimeKind
import com.smelnikowww.domain.SessionConfig
import com.smelnikowww.domain.SessionState
import com.smelnikowww.domain.TimerEngine
import com.smelnikowww.platform.AndroidMonotonicClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Owns the running session. A foreground service keeps the process alive
 * while the screen is locked, and a partial wake lock keeps the CPU awake so
 * interval chimes are not delayed by doze.
 */
class MeditationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine: TimerEngine by lazy { TimerEngine(AndroidMonotonicClock, scope) }
    private val store: ActiveSessionStore by lazy { ActiveSessionStore(this) }
    private val notifications: NotificationFactory by lazy { NotificationFactory(this) }
    private val soundPlayer: SoundPlayer by lazy { SoundPlayer(this, scope) }

    private var stateJob: Job? = null
    private var chimeJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var foregroundStarted = false
    private var finishedNotified = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannel()
        stateJob = scope.launch {
            engine.state.collect { state ->
                SessionHolder.set(state)
                when {
                    state == null -> Unit
                    state.isFinished -> onFinished(state)
                    foregroundStarted -> updateNotification(state)
                }
            }
        }
        chimeJob = scope.launch {
            engine.chimes.collect { kind ->
                val sound = if (kind == ChimeKind.END) SoundSource.CLOSING else SoundSource.BELL
                Log.i(TAG, "chime $kind -> ${sound.name}")
                soundPlayer.play(sound)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_RESTORE) {
            ACTION_START -> {
                promoteToForeground()
                val config = intent?.toSessionConfig() ?: SessionConfig.Default
                scope.launch { startOrRestore(config) }
            }

            ACTION_RESTORE -> {
                promoteToForeground()
                scope.launch { restoreOnly() }
            }

            ACTION_PAUSE -> {
                promoteToForeground()
                engine.pause()
                persist()
            }

            ACTION_RESUME -> {
                promoteToForeground()
                engine.resume()
                persist()
            }

            ACTION_STOP -> handleStop()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        soundPlayer.release()
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun startOrRestore(config: SessionConfig) {
        if (engine.hasSession) return
        val record = store.load()
        val restored = record != null && engine.restore(record)
        if (restored) {
            Log.i(TAG, "Restored session, remaining=${engine.state.value?.remainingSeconds}s")
        } else {
            if (record != null) store.clear()
            engine.start(config)
            Log.i(TAG, "Started new session, duration=${config.durationMinutes} min")
        }
        acquireWakeLock(engine.record()?.config ?: config)
        persist()
    }

    private suspend fun restoreOnly() {
        if (engine.hasSession) return
        val record = store.load()
        if (record == null || !engine.restore(record)) {
            store.clear()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        acquireWakeLock(record.config)
        persist()
        Log.i(TAG, "Restored session from service restart, remaining=${engine.state.value?.remainingSeconds}s")
    }

    private fun handleStop() {
        engine.stop()
        SessionHolder.set(null)
        releaseWakeLock()
        scope.launch { store.clear() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onFinished(state: SessionState) {
        if (finishedNotified) return
        finishedNotified = true
        scope.launch { store.clear() }
        notify(notifications.buildFinished(state))
        // Keep the service (and its wake lock) alive until the closing bell
        // has finished ringing, otherwise the tail is cut off.
        scope.launch {
            delay(CLOSING_TAIL_MILLIS)
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun promoteToForeground() {
        if (foregroundStarted) return
        val notification = notifications.buildPreparing()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationFactory.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NotificationFactory.NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
    }

    private fun updateNotification(state: SessionState) {
        notify(notifications.buildSession(state))
    }

    private fun notify(notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(this)
                .notify(NotificationFactory.NOTIFICATION_ID, notification)
        } catch (error: SecurityException) {
            Log.w(TAG, "Notification not permitted", error)
        }
    }

    private fun persist() {
        scope.launch {
            engine.record()?.let { store.save(it) }
        }
    }

    private fun acquireWakeLock(config: SessionConfig) {
        if (wakeLock?.isHeld == true) return
        val manager = getSystemService(PowerManager::class.java) ?: return
        val lock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tishina::Session")
        lock.setReferenceCounted(false)
        val timeout = config.durationMinutes * 60_000L + SAFETY_MARGIN_MILLIS
        lock.acquire(timeout)
        wakeLock = lock
        Log.i(TAG, "Partial wake lock acquired for ${config.durationMinutes} min session")
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun Intent.toSessionConfig(): SessionConfig = SessionConfig(
        durationMinutes = getIntExtra(EXTRA_DURATION, SessionConfig.Default.durationMinutes),
        intervalEnabled = getBooleanExtra(
            EXTRA_INTERVAL_ENABLED,
            SessionConfig.Default.intervalEnabled,
        ),
        intervalMinutes = getIntExtra(EXTRA_INTERVAL, SessionConfig.Default.intervalMinutes),
    ).normalized()

    companion object {
        private const val TAG = "TishinaService"
        private const val SAFETY_MARGIN_MILLIS = 5 * 60_000L
        private const val CLOSING_TAIL_MILLIS = 5_000L

        const val ACTION_START = "com.smelnikowww.action.START"
        const val ACTION_RESTORE = "com.smelnikowww.action.RESTORE"
        const val ACTION_PAUSE = "com.smelnikowww.action.PAUSE"
        const val ACTION_RESUME = "com.smelnikowww.action.RESUME"
        const val ACTION_STOP = "com.smelnikowww.action.STOP"

        const val EXTRA_DURATION = "duration_minutes"
        const val EXTRA_INTERVAL_ENABLED = "interval_enabled"
        const val EXTRA_INTERVAL = "interval_minutes"

        fun start(context: Context, config: SessionConfig) {
            val intent = Intent(context, MeditationService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_DURATION, config.durationMinutes)
                .putExtra(EXTRA_INTERVAL_ENABLED, config.intervalEnabled)
                .putExtra(EXTRA_INTERVAL, config.intervalMinutes)
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) = sendAction(context, ACTION_PAUSE)

        fun resume(context: Context) = sendAction(context, ACTION_RESUME)

        fun stop(context: Context) = sendAction(context, ACTION_STOP)

        private fun sendAction(context: Context, action: String) {
            val intent = Intent(context, MeditationService::class.java).setAction(action)
            context.startService(intent)
        }
    }
}
