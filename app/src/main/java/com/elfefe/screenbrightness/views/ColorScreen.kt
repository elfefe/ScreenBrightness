package com.elfefe.screenbrightness.views

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elfefe.screenbrightness.MainActivity
import com.elfefe.screenbrightness.SpecialColor
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MainActivity.ColorScreen() {
    var luminancePickerWidth by remember { mutableStateOf(0) }
    var updateColor by remember { mutableStateOf(color.value.hashCode()) }

    var color by remember { mutableStateOf(color.value) }

    val colorSelection by remember {
        mutableStateOf(
            listOf(
                SpecialColor(
                    Color(0xFFF5F5DC),
                    "Beige",
                    "Softer than pure white, reduce glare while maintaining readability."
                ),
                SpecialColor(
                    Color(0xFF2E2E2E),
                    "Dark Gray",
                    "Ideal for low-light environments, reducing the amount of light emitted by the screen."
                ),
                SpecialColor(
                    Color(0xFFFFE4B5),
                    "Warm Tone",
                    "Warmer hue can mitigate the effects of blue light, which can disrupt sleep patterns and cause eye strain."
                ),
                SpecialColor(
                    Color(0xFFB0E0E6),
                    "Cool Tone",
                    "Powder blue is less harsh on the eyes compared to highly saturated colors."
                ),
                SpecialColor(
                    Color(0xFF90EE90),
                    "Soft Green",
                    "Green is often considered restful for the eyes."
                ),
                SpecialColor(
                    Color(0xFF000000),
                    "Pure Black",
                    "Extreme contrasts can cause eye fatigue; opt for Dark Gray instead."
                ),
                SpecialColor(
                    Color(0xFFFFFFFF),
                    "Pure White",
                    "Extreme contrasts can cause eye fatigue; opt for Beige instead."
                ),
            )
        )
    }

    LaunchedEffect(updateColor) {
        adjustColor(color)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "Overlay color", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(32.dp))

        ColorWheel(currentColor = color.toColor()) {
            color = com.elfefe.screenbrightness.Color.fromColor(color.luminance, it)
            updateColor = color.hashCode()
        }
        Spacer(modifier = Modifier.height(32.dp))

        Canvas(
            modifier = Modifier
                .onGloballyPositioned {
                    luminancePickerWidth = it.size.width
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        color = color.apply { luminance = change.position.x / luminancePickerWidth.toFloat() }
                        updateColor = color.hashCode()
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        color = color.apply { luminance = offset.x / luminancePickerWidth.toFloat() }
                        updateColor = color.hashCode()
                    }
                }
                .fillMaxWidth(0.8f)
                .height(50.dp)
        ) {
            color.luminance.let {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Black, color.toSaturatedColor()),
                        startX = 16f,
                        endX = size.width - 32f
                    ),
                    cornerRadius = CornerRadius(32f, 32f)
                )

                drawCircle(
                    color = Color.White,
                    radius = 32f,
                    center = Offset(
                        (it * size.width).coerceIn(0f, size.width),
                        size.height / 2f
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Color selection", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(colorSelection) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.Top)
                    ) {
                        Text(text = it.name, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = it.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    ElevatedButton(
                        onClick = {
                            color = com.elfefe.screenbrightness.Color.fromColor(it.color)
                            updateColor = color.hashCode()
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.CenterVertically),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = it.color)
                    ) { }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ColorWheel(
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
    currentColor: Color = Color.White,
    onColorSelected: (Color) -> Unit
) {
    val density = LocalDensity.current
    val imageSize = with(density) { size.toPx().roundToInt() }
    val imageBitmap = remember {
        generateColorWheelBitmap(size = imageSize)
    }
    val radius = imageSize / 2f

    fun pixelAtOffset(offset: Offset) {
        val dx = offset.x - radius
        val dy = offset.y - radius
        val distance = sqrt(dx * dx + dy * dy)
        if (distance <= radius) {
            val angle = (atan2(dy, dx) * (180f / PI.toFloat()) + 360f) % 360f // 0 to 360
            val saturation = distance / radius // 0 to 1
            val hue = angle
            val selectedColor = Color.hsv(hue, saturation, 1f)
            onColorSelected(selectedColor)
        }
    }

    var cursorPosition = colorToPosition(currentColor, radius, Offset(radius, radius))
        ?: Offset(radius, radius)

    // Modifier to handle touch input
    val touchModifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                // Wait for touch down event
                val down = awaitPointerEvent().changes.firstOrNull() ?: continue

                if (down.pressed) {
                    // Consume the down event to prevent the Pager from intercepting
                    down.consume()

                    // Handle drag events
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        // Calculate position relative to center
                        pixelAtOffset(change.position)
                        cursorPosition = change.position

                        // Consume the move event
                        change.consume()
                    } while (event.changes.any { it.pressed })

                    // Touch released
                }
            }
        }
    }

    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        modifier = Modifier
            .drawWithContent {
                drawContent()
                drawCircle(
                    color = Color.White,
                    radius = 32f,
                    center = cursorPosition
                )
            }
            .size(size)
            .then(touchModifier)
            .then(modifier)
    )
}

fun generateColorWheelBitmap(size: Int): ImageBitmap {
    val bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val radius = size / 2f

    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val dx = x - radius
            val dy = y - radius
            val distance = sqrt(dx * dx + dy * dy)
            if (distance <= radius) {
                val angle =
                    (atan2(dy, dx) * (180f / PI.toFloat()) + 360f) % 360f // Angle from 0 to 360
                val h = angle // Hue from 0 to 360
                val s = distance / radius // Saturation from 0 to 1
                val v = 1f // Value
                val color = Color.hsv(h, s, v).toArgb()
                pixels[y * size + x] = color
            } else {
                // Outside the circle, set transparent or background color
                pixels[y * size + x] = Color.Transparent.toArgb()
            }
        }
    }
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap.asImageBitmap()
}

fun colorToPosition(
    color: Color,
    radius: Float,
    center: Offset
): Offset? {
    // Decompose the color into HSV components
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    val hue = hsv[0]      // 0 to 360
    val saturation = hsv[1] // 0 to 1
    val value = hsv[2]      // 0 to 1

    // If value is 0, the color is black and not on the wheel
    if (value == 0f || saturation == 0f) {
        return null // The color is not on the color wheel
    }

    // Calculate angle in radians
    val angleRadians = Math.toRadians(hue.toDouble())

    // Calculate distance from center
    val distance = saturation * radius

    // Convert polar coordinates to Cartesian coordinates
    val x = center.x + (distance * cos(angleRadians)).toFloat()
    val y = center.y + (distance * sin(angleRadians)).toFloat()

    return Offset(x, y)
}

