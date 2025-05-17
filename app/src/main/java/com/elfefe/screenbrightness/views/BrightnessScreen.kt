package com.elfefe.screenbrightness.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elfefe.screenbrightness.R


@Composable
fun BrightnessScreen(
    initialBrightness: Int, onBrightnessChange: (Int) -> Unit,
    initialBrighnessStep: Int, onBrightnessStepChange: (Int) -> Unit
) {
    var brightness by remember { mutableStateOf(initialBrightness) }
    var brightnessStep by remember { mutableStateOf(initialBrighnessStep) }

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.screen_brightness), style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${((brightness / 255f) * 100).toInt().coerceIn(0, 100)}%",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Slider(
            value = brightness.toFloat(),
            onValueChange = {
                brightness = it.toInt()
                onBrightnessChange(255 - brightness)
            },
            valueRange = 0f..255f
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.notification_brightness_step), style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${brightnessStep.toInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Slider(
            value = brightnessStep.toFloat(),
            onValueChange = {
                brightnessStep = it.toInt()
                onBrightnessStepChange(brightnessStep)
            },
            valueRange = 1f..85f
        )
    }
}

