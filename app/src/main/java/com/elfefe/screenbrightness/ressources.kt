package com.elfefe.screenbrightness

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.math.round

fun Number.round(decimals: Int): Float {
    var multiplier = 10.0.pow(decimals.toDouble()).toFloat()
    return round(this.toFloat() * multiplier) / multiplier
}

data class SpecialColor(
    val color: Color,
    val name: String,
    val description: String
)