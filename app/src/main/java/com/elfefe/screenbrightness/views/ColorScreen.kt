package com.elfefe.screenbrightness.views

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elfefe.screenbrightness.ColorSaver
import com.elfefe.screenbrightness.R
import com.elfefe.screenbrightness.SpecialColor
import com.elfefe.screenbrightness.resString
import com.elfefe.screenbrightness.toSpecialColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Composable function that displays the color selection screen.
 * Allows the user to pick a color using a color wheel and a luminance slider, or select from predefined colors.
 *
 * @receiver The [MainActivity] instance, providing access to its properties and methods for adjusting the overlay color.
 */
@Preview
@Composable
fun ColorScreen(modifier: Modifier = Modifier, updateColor: com.elfefe.screenbrightness.Color = Color.Black.toSpecialColor(), onUpdateColor: (com.elfefe.screenbrightness.Color) -> Unit = {}) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(modifier),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            WheelView(Modifier.weight(1f), updateColor, onUpdateColor)
            Spacer(modifier = Modifier.width(16.dp))
            ColorsView(Modifier.weight(1f), updateColor, onUpdateColor)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            WheelView(Modifier, updateColor, onUpdateColor)
            Spacer(modifier = Modifier.height(16.dp))
            ColorsView(Modifier, updateColor, onUpdateColor)
        }
    }
}

@Composable
fun WheelView(modifier: Modifier, updateColor: com.elfefe.screenbrightness.Color, onUpdateColor: (com.elfefe.screenbrightness.Color) -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var luminancePickerWidth by rememberSaveable { mutableStateOf(0) }

    var showColors by rememberSaveable { mutableStateOf(true) }
    val buttonColorsRotation by animateFloatAsState(targetValue = if (showColors) 180f else 0f, label = "")
    val spaceColors by animateDpAsState(targetValue = if (showColors) 32.dp else 0.dp, label = "")

    println("WheelView ${updateColor.hashCode()}")

    Column(modifier = Modifier.then(modifier)) {
        AnimatedVisibility(showColors, modifier = Modifier.size(512.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Text(
                    text = stringResource(R.string.overlay_color),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(32.dp))

                ColorWheel(currentColor = updateColor.toColor()) {
                    onUpdateColor(com.elfefe.screenbrightness.Color.fromColor(updateColor.luminance, it))
                }
                Spacer(modifier = Modifier.height(32.dp))

                Canvas(
                    modifier = Modifier
                        .onGloballyPositioned {
                            luminancePickerWidth = it.size.width
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                onUpdateColor(
                                    com.elfefe.screenbrightness.Color.fromColor(
                                        luminance = change.position.x / luminancePickerWidth.toFloat(),
                                        updateColor.toColor()
                                    )
                                )
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                onUpdateColor(
                                    com.elfefe.screenbrightness.Color.fromColor(
                                        luminance = offset.x / luminancePickerWidth.toFloat(),
                                        updateColor.toColor()
                                    )
                                )
                            }
                        }
                        .fillMaxWidth(0.8f)
                        .height(50.dp)
                ) {
                    with(updateColor) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Black, toSaturatedColor()),
                                    startX = 16f,
                                    endX = size.width - 32f
                                ),
                                cornerRadius = CornerRadius(32f, 32f)
                            )

                            drawCircle(
                                color = Color.White,
                                radius = 32f,
                                center = Offset(
                                    (luminance * size.width).coerceIn(0f, size.width),
                                    size.height / 2f
                                )
                            )
                    }
                }
            }
        }

        if (isLandscape) return@Column

        Spacer(modifier = Modifier.height(spaceColors))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.color_selection),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.width(32.dp))
            IconButton({
                showColors = !showColors
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Handle",
                    modifier = Modifier
                        .rotate(buttonColorsRotation)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ColorsView(modifier: Modifier, updateColor: com.elfefe.screenbrightness.Color, onUpdateColor: (com.elfefe.screenbrightness.Color) -> Unit) {
    val colorSelection by rememberSaveable {
        mutableStateOf(
            listOf(
                SpecialColor(
                    Color(0xFFF5F5DC),
                    resString(R.string.color_beige),
                    resString(R.string.color_beige_description)
                ),
                SpecialColor(
                    Color(0xFF2E2E2E),
                    resString(R.string.color_dark_gray),
                    resString(R.string.color_dark_gray_description)
                ),
                SpecialColor(
                    Color(0xFFFFE4B5),
                    resString(R.string.color_warm_tone),
                    resString(R.string.color_warm_tone_description)
                ),
                SpecialColor(
                    Color(0xFFB0E0E6),
                    resString(R.string.color_cool_tone),
                    resString(R.string.color_cool_tone_description)
                ),
                SpecialColor(
                    Color(0xFF90EE90),
                    resString(R.string.color_soft_green),
                    resString(R.string.color_soft_green_description)
                ),
                SpecialColor(
                    Color(0xFF000000),
                    resString(R.string.color_pure_black),
                    resString(R.string.color_pure_black_description)
                ),
                SpecialColor(
                    Color(0xFFFFFFFF),
                    resString(R.string.color_pure_white),
                    resString(R.string.color_pure_white_description)
                ),
            )
        )
    }

    LazyColumn(
        modifier = Modifier.then(modifier),
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
                        onUpdateColor(it.color.toSpecialColor())
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

/**
 * Composable function that displays a color wheel for selecting a color.
 *
 * @param modifier Modifier for this composable.
 * @param size The size of the color wheel.
 * @param currentColor The currently selected color.
 * @param onColorSelected Callback invoked when a new color is selected from the wheel.
 */
@Composable
fun ColorWheel(
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
    currentColor: Color = Color.White,
    onColorSelected: (Color) -> Unit
) {
    val scope = rememberCoroutineScope()
    var density = LocalDensity.current
    var imageBitmap: ImageBitmap? by remember { mutableStateOf(null) }
    val imageSize = with(density) { size.toPx().roundToInt() }
    val radius = imageSize / 2f
    var cursorPosition by remember { mutableStateOf(Offset(radius, radius)) }
    var touchModifier by remember { mutableStateOf(Modifier) }

    LaunchedEffect("ColorWheel") {
        scope.launch(Dispatchers.Default) {
            imageBitmap =
                generateColorWheelBitmap(size = imageSize)

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

            cursorPosition = colorToPosition(currentColor, radius, Offset(radius, radius))
                ?: Offset(radius, radius)

            // Modifier to handle touch input
            touchModifier.pointerInput(Unit) {
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
                                println("pixelAtOffset called")
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
        }
    }

    imageBitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val radius = imageSize / 2f
                        val dx = offset.x - radius
                        val dy = offset.y - radius
                        val distance = sqrt(dx * dx + dy * dy)
                        if (distance <= radius) {
                            val angle =
                                (atan2(dy, dx) * (180f / PI.toFloat()) + 360f) % 360f // 0 to 360
                            val saturation = distance / radius // 0 to 1
                            val hue = angle
                            val selectedColor = Color.hsv(hue, saturation, 1f)
                            onColorSelected(selectedColor)
                            cursorPosition = offset
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        val offset = change.position
                        val radius = imageSize / 2f
                        val dx = offset.x - radius
                        val dy = offset.y - radius
                        val distance = sqrt(dx * dx + dy * dy)
                        if (distance <= radius) {
                            val angle =
                                (atan2(dy, dx) * (180f / PI.toFloat()) + 360f) % 360f // 0 to 360
                            val saturation = distance / radius // 0 to 1
                            val hue = angle
                            val selectedColor = Color.hsv(hue, saturation, 1f)
                            onColorSelected(selectedColor)
                            cursorPosition = offset
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    drawCircle(
                        color = Color.White,
                        radius = 32f,
                        center = cursorPosition
                    )
                }
                .then(modifier)
        )
    }
}

/**
 * Generates a [Bitmap] image of a color wheel.
 *
 * @param size The width and height of the bitmap to generate.
 * @return An [ImageBitmap] representing the color wheel.
 */
fun generateColorWheelBitmap(size: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
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
                val s = (distance / radius).coerceIn(0f, 1f) // Saturation from 0 to 1
                val v = 1f // Value
                val color = Color.hsv(h, s, v).toArgb()
                pixels[y * size + x] = color
            }  else {
                // Outside the circle, set transparent or background color
                pixels[y * size + x] = Color.Transparent.toArgb()
            }
        }
    }
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap.asImageBitmap()
}

/**
 * Converts a given [Color] to its corresponding [Offset] position on a color wheel.
 *
 * @param color The color to convert.
 * @param radius The radius of the color wheel.
 * @param center The center offset of the color wheel.
 * @return The [Offset] position of the color on the wheel, or null if the color is not on the wheel (e.g., black or white).
 */
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
