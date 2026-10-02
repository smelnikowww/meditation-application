package com.smelnikowww.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smelnikowww.domain.SessionConfig
import com.smelnikowww.domain.formatMinutesLabel
import com.smelnikowww.ui.components.GradientValueText
import com.smelnikowww.ui.components.MinuteSlider
import com.smelnikowww.ui.components.PresetRow
import com.smelnikowww.ui.components.SectionHeader

@Composable
fun IntervalPicker(
    enabled: Boolean,
    minutes: Int,
    onEnabledChange: (Boolean) -> Unit,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SectionHeader("Период звука")
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
        Spacer(Modifier.height(4.dp))
        if (enabled) {
            GradientValueText(value = formatMinutesLabel(minutes))
            Spacer(Modifier.height(12.dp))
            PresetRow(
                items = SessionConfig.IntervalPresets,
                selected = minutes,
                label = ::formatMinutesLabel,
                onSelect = onMinutesChange,
            )
            Spacer(Modifier.height(4.dp))
            MinuteSlider(
                value = minutes,
                onValueChange = onMinutesChange,
                valueRange = SessionConfig.MIN_INTERVAL..SessionConfig.MAX_INTERVAL,
                step = SessionConfig.INTERVAL_STEP,
            )
        } else {
            GradientValueText(
                value = "Без звука",
                fontSize = 30.sp,
            )
        }
    }
}
