package com.elfefe.screenbrightness

import android.app.Application
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elfefe.screenbrightness.MainActivity.Companion.MAX_BRIGHTNESS
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

fun Number.round(decimals: Int): Float {
    var multiplier = 10.0.pow(decimals.toDouble()).toFloat()
    return round(this.toFloat() * multiplier) / multiplier
}

fun resString(res: Int, vararg format: Any) = try {
    com.elfefe.screenbrightness.Application.instance.getString(res, format)
} catch (_: Exception) { "Previw-String($res)" }

fun Color.toSpecialColor() = com.elfefe.screenbrightness.Color.fromColor(this)

@Composable
fun Header(text: String) {
    Text(text,
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
fun SubHeader(text: String) {
    Text(text,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp)
    )
}

@Composable
fun CustomOutlinedButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp, brush = SolidColor(MaterialTheme.colorScheme.primary)),
        content = content
    )
}

@Composable
fun CustomButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        content = content
    )
}

fun Int.colorToPercent() = ((this / 255f) * 100).roundToInt()

data class SpecialColor(
    val color: Color,
    val name: String,
    val description: String
)

