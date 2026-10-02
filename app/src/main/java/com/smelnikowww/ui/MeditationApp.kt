package com.smelnikowww.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smelnikowww.data.ActiveSessionStore
import com.smelnikowww.domain.SessionConfig
import com.smelnikowww.platform.AndroidMonotonicClock
import com.smelnikowww.service.SessionHolder
import com.smelnikowww.ui.session.DoneScreen
import com.smelnikowww.ui.session.SessionScreen
import com.smelnikowww.ui.setup.SetupScreen

object Routes {
    const val SETUP = "setup"
    const val SESSION = "session"
    const val DONE = "done"
}

@Composable
fun MeditationApp(navController: NavHostController = rememberNavController()) {
    var pendingConfig by remember { mutableStateOf<SessionConfig?>(null) }
    val context = LocalContext.current
    val store = remember(context) { ActiveSessionStore(context) }

    // Resume a session that survived process death (e.g. screen was locked).
    LaunchedEffect(Unit) {
        val record = store.load() ?: return@LaunchedEffect
        val now = AndroidMonotonicClock.elapsedRealtimeMillis()
        if (record.startedAtMillis > now) {
            // Monotonic base changed after a reboot; the record is unusable.
            store.clear()
            return@LaunchedEffect
        }
        val reference = record.pauseStartedAtMillis ?: now
        val elapsed = (reference - record.startedAtMillis - record.accumulatedPauseMillis)
            .coerceAtLeast(0L)
        val alreadyFinished = record.pauseStartedAtMillis == null &&
            elapsed >= record.config.durationMinutes * 60_000L
        if (alreadyFinished) {
            store.clear()
            return@LaunchedEffect
        }
        pendingConfig = record.config
        navController.navigate(Routes.SESSION) { launchSingleTop = true }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SETUP,
    ) {
        composable(Routes.SETUP) {
            SetupScreen(
                onStart = { config ->
                    pendingConfig = config
                    navController.navigate(Routes.SESSION) { launchSingleTop = true }
                },
            )
        }
        composable(Routes.SESSION) {
            SessionScreen(
                config = pendingConfig ?: SessionConfig.Default,
                onFinished = {
                    navController.navigate(Routes.DONE) {
                        popUpTo(Routes.SESSION) { inclusive = true }
                    }
                },
                onExit = {
                    navController.popBackStack(Routes.SETUP, inclusive = false)
                },
            )
        }
        composable(Routes.DONE) {
            DoneScreen(
                onRepeat = {
                    // Drop the finished state so the session screen starts fresh
                    // instead of immediately bouncing back to the summary.
                    SessionHolder.set(null)
                    navController.navigate(Routes.SESSION) {
                        popUpTo(Routes.DONE) { inclusive = true }
                    }
                },
                onDone = {
                    SessionHolder.set(null)
                    navController.popBackStack(Routes.SETUP, inclusive = false)
                },
            )
        }
    }
}
