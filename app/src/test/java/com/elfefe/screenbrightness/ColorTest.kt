package com.elfefe.screenbrightness

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ColorTest {
    @Test
    fun `test color int conversion work correctly`() {
        val colorMax = Color(
            1f,
            1f,
            Color.Saturation(
                1f,
                1f,
                1f
            )
        )
        val colorMin = Color(
            0f,
            0f,
            Color.Saturation(
                0f,
                0f,
                0f
            )
        )
        val colorMid = Color(
            0.5f,
            1f,
            Color.Saturation(
                0.5f,
                0.5f,
                0.5f
            )
        )
        val colorMid2 = Color(
            0.17f,
            1f,
            Color.Saturation(
                1f,
                0.996f,
                0.894f
            )
        )
        val reColorMax = Color.fromLong(colorMax.toLong())
        val reColorMin = Color.fromLong(colorMin.toLong())
        val reColorMid = Color.fromLong(colorMid.toLong())
        val reColorMid2 = Color.fromLong(colorMid2.toLong())

        println(colorMid2)
        println(reColorMid2)
        assert(colorMax == reColorMax)
        assert(colorMin == reColorMin)
        assert(colorMid == reColorMid)
    }

    @Test
    fun `test rounding`() {
        assert(1.2345.round(0) == 1.0f)
        assert(1.2345.round(1) == 1.2f)
        assert(1.2345.round(2) == 1.23f)
        assert(1.2345.round(3) == 1.234f)
    }
}