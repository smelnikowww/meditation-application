package com.smelnikowww.ui.components

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt

/**
 * Integer slider that snaps to a fixed step inside an inclusive range.
 */
@Composable
fun MinuteSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    step: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val span = valueRange.last - valueRange.first
    val steps = (span / step - 1).coerceAtLeast(0)
    Slider(
        value = value.toFloat(),
        onValueChange = { raw -> onValueChange(snapToStep(raw, valueRange, step)) },
        valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
        steps = steps,
        enabled = enabled,
        modifier = modifier,
    )
}

private fun snapToStep(raw: Float, range: IntRange, step: Int): Int {
    val stepsFromMin = ((raw - range.first) / step).roundToInt()
    return (range.first + stepsFromMin * step).coerceIn(range.first, range.last)
}
