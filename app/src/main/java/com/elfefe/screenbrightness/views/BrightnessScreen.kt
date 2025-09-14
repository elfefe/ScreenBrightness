package com.elfefe.screenbrightness.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
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
import com.elfefe.screenbrightness.CustomButton
import com.elfefe.screenbrightness.Header
import com.elfefe.screenbrightness.MainActivity.Companion.MAX_BRIGHTNESS
import com.elfefe.screenbrightness.MainActivity.Companion.MIN_BRIGHTNESS
import com.elfefe.screenbrightness.R
import com.elfefe.screenbrightness.SubHeader
import kotlin.math.roundToInt


/**
 * Composable function that displays controls for adjusting screen brightness and notification step.
 *
 * @param initialBrightness The initial brightness value (0-255).
 * @param onBrightnessChange Callback invoked when the brightness value changes.
 * @param initialBrighnessStep The initial brightness step value for notification controls.
 * @param onBrightnessStepChange Callback invoked when the brightness step value changes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrightnessScreen(
    initialBrightness: Int, onBrightnessChange: (Int) -> Unit,
    initialBrighnessStep: Int, onBrightnessStepChange: (Int) -> Unit
) {
    // Remember the brightness and brightnessStep values to be able to update them locally.
    var brightness by remember { mutableStateOf(initialBrightness) }
    var brightnessStep by remember { mutableStateOf(initialBrighnessStep) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        stickyHeader {
            Header(stringResource(R.string.filter))
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            // Screen brightness slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubHeader (stringResource(R.string.assombrissement_du_filtre))
                Text(
                    // Display brightness as a percentage
                    text = "${
                        ((brightness.toFloat() / MAX_BRIGHTNESS) * 100).roundToInt()
                            .coerceIn(0, 100)
                    }%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Slider(
                value = brightness.toFloat(),
                onValueChange = {
                    brightness = it.toInt()
                    // Invert brightness value because the overlay is a dark layer
                    onBrightnessChange(brightness)
                },
                valueRange = MIN_BRIGHTNESS.toFloat()..MAX_BRIGHTNESS.toFloat()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomButton(
                    {
                        brightness = (0.1f * MAX_BRIGHTNESS).roundToInt()
                        // Invert brightness value because the overlay is a dark layer
                        onBrightnessChange(brightness)
                    }
                ) {
                    Text(stringResource(R.string.faible))
                }

                CustomButton(
                    {
                        brightness = (0.2f * MAX_BRIGHTNESS).roundToInt()
                        // Invert brightness value because the overlay is a dark layer
                        onBrightnessChange(brightness)
                    }
                ) {
                    Text(stringResource(R.string.conseille))
                }

                CustomButton(
                    {
                        brightness = (0.8f * MAX_BRIGHTNESS).roundToInt()
                        // Invert brightness value because the overlay is a dark layer
                        onBrightnessChange(brightness)
                    }
                ) {
                    Text(stringResource(R.string.forte))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        stickyHeader {
            Header(stringResource(R.string.notification))
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Notification brightness step slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubHeader (stringResource(R.string.notification_brightness_step))
                Text(
                    text = "${brightnessStep}%",
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
                steps = 10,
                // Step value is a percentage from 1 to 99 (to avoid too large steps)
                valueRange = 1f..99f
            )
        }
    }
}

