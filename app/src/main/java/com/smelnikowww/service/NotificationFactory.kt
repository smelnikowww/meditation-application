package com.smelnikowww.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.smelnikowww.MainActivity
import com.smelnikowww.R
import com.smelnikowww.domain.SessionState
import com.smelnikowww.domain.formatClock

/** Builds the ongoing session notification and its control actions. */
class NotificationFactory(private val context: Context) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Медитация",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Идущая сессия медитации"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun buildPreparing(): Notification = base(contentText = "Подготовка…")
        .setOngoing(true)
        .build()

    fun buildSession(state: SessionState): Notification {
        val builder = base(
            contentText = if (state.isPaused) {
                "Пауза · осталось ${formatClock(state.remainingSeconds)}"
            } else {
                "Медитация идёт · осталось ${formatClock(state.remainingSeconds)}"
            },
        ).setOngoing(true)

        if (!state.isPaused) {
            builder
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + state.remainingSeconds * 1000L)
        }

        if (state.isPaused) {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Продолжить",
                serviceAction(MeditationService.ACTION_RESUME, REQUEST_RESUME),
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Пауза",
                serviceAction(MeditationService.ACTION_PAUSE, REQUEST_PAUSE),
            )
        }
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Стоп",
            serviceAction(MeditationService.ACTION_STOP, REQUEST_STOP),
        )
        return builder.build()
    }

    fun buildFinished(state: SessionState): Notification = base(
        contentText = "Сессия завершена · ${formatClock(state.config.durationMinutes * 60L)}",
    )
        .setOngoing(false)
        .setAutoCancel(true)
        .build()

    private fun base(contentText: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tishina)
            .setContentTitle("Тишина")
            .setContentText(contentText)
            .setColor(0xFF6FD6C4.toInt())
            .setContentIntent(openAppIntent())
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setSilent(true)

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun serviceAction(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MeditationService::class.java).setAction(action)
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val CHANNEL_ID = "meditation_session"
        const val NOTIFICATION_ID = 4201
        private const val REQUEST_OPEN = 10
        private const val REQUEST_PAUSE = 11
        private const val REQUEST_RESUME = 12
        private const val REQUEST_STOP = 13
    }
}
