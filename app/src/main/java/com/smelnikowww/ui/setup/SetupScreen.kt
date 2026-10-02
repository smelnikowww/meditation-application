package com.smelnikowww.ui.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smelnikowww.R
import com.smelnikowww.data.SessionPreferences
import com.smelnikowww.domain.SessionConfig
import com.smelnikowww.domain.formatCount
import com.smelnikowww.domain.formatMinutesLabel
import com.smelnikowww.ui.components.BrandBackground

@Composable
fun SetupScreen(
    onStart: (SessionConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferences = remember(context) { SessionPreferences(context) }
    val viewModel: SetupViewModel = viewModel(factory = SetupViewModel.factory(preferences))
    val config by viewModel.config.collectAsStateWithLifecycle()

    BrandBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            SetupHeader()
            Spacer(Modifier.height(28.dp))
            DurationPicker(
                minutes = config.durationMinutes,
                onMinutesChange = viewModel::setDuration,
            )
            Spacer(Modifier.height(28.dp))
            IntervalPicker(
                enabled = config.intervalEnabled,
                minutes = config.intervalMinutes,
                onEnabledChange = viewModel::setIntervalEnabled,
                onMinutesChange = viewModel::setInterval,
            )
            Spacer(Modifier.height(28.dp))
            SessionSummary(config = config)
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { onStart(config) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = config.isValid,
            ) {
                Text("Начать", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SetupHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_logo_mark),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = "Тишина",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SessionSummary(
    config: SessionConfig,
    modifier: Modifier = Modifier,
) {
    val soundLine = when {
        !config.intervalEnabled -> "Без звука"
        config.intervalChimeCount == 0 -> "Звук один раз в конце"
        else -> "Звук каждые ${formatMinutesLabel(config.intervalMinutes)}, " +
            formatCount(config.intervalChimeCount, "раз", "раза", "раз")
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Сессия ${formatMinutesLabel(config.durationMinutes)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = soundLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
