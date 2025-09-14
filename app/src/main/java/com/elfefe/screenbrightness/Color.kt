package com.elfefe.screenbrightness

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

val ColorSaver: Saver<com.elfefe.screenbrightness.Color, Long> = Saver(
    save = { it.hashCode().toLong() }, // Color packs itself into a Long ARGB
    restore = { com.elfefe.screenbrightness.Color.fromLong(it)
    }
)

class Color(
    luminance: Float, // Between 0 and 1
    alpha: Float,     // Between 0 and 1
    var saturation: Saturation
) {
    var luminance: Float = luminance
        get() = field.round(2).absoluteValue
        set(value) {
            field = value.round(2).coerceIn(0f, 1f)
        }
    var alpha: Float = alpha
        set(value) {
            field = value.round(2).coerceIn(0f, 1f)
        }

    val red: Int
        get() = (saturation.red * 255 * luminance).toInt()
    val green: Int
        get() = (saturation.green * 255 * luminance).toInt()
    val blue: Int
        get() = (saturation.blue * 255 * luminance).toInt()

    // Convert color to an Int representation (similar to android.graphics.Color)
    fun toLong(): Long {
        val a = (alpha * 255).toLong().coerceIn(0, 255)
        val l = (luminance * 255).toLong().coerceIn(0, 255)
        val r = (saturation.red * 255).toLong()
        val g = (saturation.green * 255).toLong()
        val b = (saturation.blue * 255).toLong()
        return (l shl 32) or (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun toColor(): androidx.compose.ui.graphics.Color {
        return Color(
            red = red / 255f,
            green = green / 255f,
            blue = blue / 255f,
            alpha = alpha
        )
    }

    fun toSaturatedColor(): Color {
        return Color(
            red = saturation.red,
            green = saturation.green,
            blue = saturation.blue,
            alpha = alpha
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is com.elfefe.screenbrightness.Color) return false

        if (luminance != other.luminance) return false
        if (alpha != other.alpha) return false
        if (saturation != other.saturation) return false

        return true
    }

    override fun toString(): String {
        return "Color(luminance=$luminance, alpha=$alpha, red=$red, green=$green, blue=$blue, saturation=$saturation)"
    }

    override fun hashCode(): Int {
        var result = saturation.hashCode()
        result = 31 * result + luminance.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + red
        result = 31 * result + green
        result = 31 * result + blue
        println("Color hashCode: $result")
        return result
    }

    class Saturation(red: Float, green: Float, blue: Float) {
        val red: Float = red.absoluteValue
            get() = field.round(3).coerceIn(0f, 1f)
        val green: Float = green.absoluteValue
            get() = field.round(3).coerceIn(0f, 1f)
        val blue: Float = blue.absoluteValue
            get() = field.round(3).coerceIn(0f, 1f)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Saturation) return false

            if (red != other.red) return false
            if (green != other.green) return false
            if (blue != other.blue) return false

            return true
        }
        override fun toString(): String = "Saturation(red=$red, green=$green, blue=$blue)"
        override fun hashCode(): Int {
            var result = red.hashCode()
            result = 31 * result + green.hashCode()
            result = 31 * result + blue.hashCode()
            return result
        }
    }

    // Static method to convert from Int to Color object
    companion object {
        fun fromColor(color: Color): com.elfefe.screenbrightness.Color {
            val maxComponent = maxOf(color.red, color.green, color.blue)
            return fromColor(maxComponent, color)
        }

        fun fromColor(luminance: Float, color: Color): com.elfefe.screenbrightness.Color {
            val maxComponent = maxOf(color.red, color.green, color.blue)
            val diffScale = (1f - maxComponent).round(2)
            return Color(
                luminance = luminance,
                alpha = color.alpha,
                saturation = Saturation(
                    red = (color.red + (color.red * diffScale)).coerceAtMost(1f),
                    green = (color.green + (color.green * diffScale)).coerceAtMost(1f),
                    blue = (color.blue + (color.blue * diffScale)).coerceAtMost(1f)
                )
            )
        }

        fun fromLong(colorInt: Long): com.elfefe.screenbrightness.Color {
            val l = (colorInt shr 32) and 0xFF
            val a = (colorInt shr 24) and 0xFF
            val r = (colorInt shr 16) and 0xFF
            val g = (colorInt shr 8) and 0xFF
            val b = colorInt and 0xFF
            return Color(
                luminance = l / 255f,
                alpha = a / 255f,
                saturation = Saturation(
                    red = r / 255f,
                    green = g / 255f,
                    blue = b / 255f
                )
            )
        }
    }
}