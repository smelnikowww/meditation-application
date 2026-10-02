package com.smelnikowww.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.smelnikowww.ui.theme.BrandGradientColors

/**
 * Large brand-gradient value readout used by the pickers.
 */
@Composable
fun GradientValueText(
    value: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 42.sp,
) {
    Text(
        text = value,
        modifier = modifier,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = fontSize,
            lineHeight = fontSize * 1.15f,
            brush = Brush.linearGradient(BrandGradientColors),
        ),
    )
}
