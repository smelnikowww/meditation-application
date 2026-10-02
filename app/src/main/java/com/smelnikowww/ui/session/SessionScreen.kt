package com.smelnikowww.ui.session

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smelnikowww.R
import com.smelnikowww.domain.SessionConfig
import com.smelnikowww.domain.formatClock
import com.smelnikowww.service.MeditationService
import com.smelnikowww.service.SessionHolder
import com.smelnikowww.ui.components.BrandBackground
import com.smelnikowww.ui.components.BreathingProgressRing
import com.smelnikowww.ui.theme.TimerDisplayTextStyle

@Composable
fun SessionScreen(
    config: SessionConfig,
    onFinished: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by SessionHolder.state.collectAsStateWithLifecycle()
    var showStopDialog by remember { mutableStateOf(false) }
    var notificationsDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsDenied = !granted }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(Unit) {
        val current = SessionHolder.state.value
        if (current == null || current.isFinished) {
            MeditationService.start(context, config)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(state?.isFinished) {
        if (state?.isFinished == true) {
            onFinished()
        }
    }

    BackHandler { onExit() }

    val active = state
    val activeConfig = active?.config ?: config
    val totalSeconds = (activeConfig.durationMinutes * 60).coerceAtLeast(1).toFloat()
    val progress = active?.let { 1f - (it.remainingSeconds / totalSeconds) } ?: 0f

    val pulse = remember { Animatable(0f) }
    val chimesPlayed = active?.chimesPlayed ?: 0
    LaunchedEffect(chimesPlayed) {
        if (chimesPlayed > 0) {
            pulse.snapTo(1f)
            pulse.animateTo(0f, animationSpec = tween(durationMillis = 1400))
        }
    }

    BrandBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Тишина",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            BreathingProgressRing(
                progress = progress,
                isPaused = active?.isPaused == true,
                pulse = pulse.value,
                modifier = Modifier.size(300.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatClock(active?.remainingSeconds ?: activeConfig.durationMinutes * 60L),
                        style = TimerDisplayTextStyle,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = when {
                            active == null -> "Запуск…"
                            active.isPaused -> "Пауза"
                            else -> "Медитация"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    active?.nextChimeSeconds?.let { next ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Звук через ${formatClock(next)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleControlButton(
                        iconRes = if (active?.isPaused == true) R.drawable.ic_play else R.drawable.ic_pause,
                        contentDescription = if (active?.isPaused == true) "Продолжить" else "Пауза",
                        filled = true,
                        size = 72.dp,
                        onClick = {
                            if (active?.isPaused == true) {
                                MeditationService.resume(context)
                            } else {
                                MeditationService.pause(context)
                            }
                        },
                    )
                    CircleControlButton(
                        iconRes = R.drawable.ic_stop,
                        contentDescription = "Завершить",
                        filled = false,
                        size = 60.dp,
                        onClick = { showStopDialog = true },
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Экран не гаснет · сессия идёт в фоне",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (notificationsDenied) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Уведомление скрыто: нет разрешения",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Завершить сессию?") },
            text = { Text("Медитация будет остановлена.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopDialog = false
                        MeditationService.stop(context)
                        onExit()
                    },
                ) { Text("Завершить") }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("Продолжить") }
            },
        )
    }
}

@Composable
private fun CircleControlButton(
    iconRes: Int,
    contentDescription: String,
    filled: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val container = if (filled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val tint = if (filled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size / 2.6f),
        )
    }
}
