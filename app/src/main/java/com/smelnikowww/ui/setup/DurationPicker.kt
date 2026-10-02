package com.smelnikowww.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smelnikowww.domain.SessionConfig
import com.smelnikowww.domain.formatMinutesLabel
import com.smelnikowww.ui.components.GradientValueText
import com.smelnikowww.ui.components.MinuteSlider
import com.smelnikowww.ui.components.PresetRow
import com.smelnikowww.ui.components.SectionHeader

@Composable
fun DurationPicker(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader("Длительность")
        Spacer(Modifier.height(4.dp))
        GradientValueText(value = formatMinutesLabel(minutes))
        Spacer(Modifier.height(12.dp))
        PresetRow(
            items = SessionConfig.DurationPresets,
            selected = minutes,
            label = ::formatMinutesLabel,
            onSelect = onMinutesChange,
        )
        Spacer(Modifier.height(4.dp))
        MinuteSlider(
            value = minutes,
            onValueChange = onMinutesChange,
            valueRange = SessionConfig.MIN_DURATION..SessionConfig.MAX_DURATION,
            step = SessionConfig.DURATION_STEP,
        )
    }
}
