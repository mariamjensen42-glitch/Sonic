package com.cycling.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HCT 色彩空间：CAM16 色相/彩度 + CIE L* 明度。
 * Material 3 的全部色调板都由它派生，是动态取色与色调映射的数学基础。
 */
@Immutable
class Hct internal constructor(
    val hue: Double,
    val chroma: Double,
    val tone: Double
) {

    /** 转回 sRGB。超出色域时按等色相降彩度映射，保证结果始终可用。 */
    fun toColor(): Color {
        if (chroma < 1.0 || tone.roundToInt() <= 0 || tone.roundToInt() >= 100) {
            return colorFromLstar(tone)
        }
        val h = ((hue % 360.0) + 360.0) % 360.0
        val j = solveJ(h, chroma, tone)
        if (isInGamut(cam16ToXyz(h, chroma, j))) return colorFromXyz(cam16ToXyz(h, chroma, j))

        // 色域外：二分收敛到色域边界上的最大可用彩度
        var low = 0.0
        var high = chroma
        repeat(GAMUT_ITERATIONS) {
            val mid = (low + high) / 2.0
            val xyz = cam16ToXyz(h, mid, solveJ(h, mid, tone))
            if (isInGamut(xyz)) low = mid else high = mid
        }
        return colorFromXyz(cam16ToXyz(h, low, solveJ(h, low, tone)))
    }

    companion object {
        private const val J_ITERATIONS = 18
        private const val GAMUT_ITERATIONS = 18

        fun fromColor(color: Color): Hct = fromArgb(color.toArgb())

        fun fromArgb(argb: Int): Hct {
            val xyz = argbToXyz(argb)
            val cam = cam16FromXyz(xyz[0], xyz[1], xyz[2])
            val grayHue = if (cam.chroma < ACHROMATIC_CHROMA) 0.0 else cam.hue
            return Hct(grayHue, cam.chroma, lstarFromY(xyz[1]))
        }

        fun from(hue: Double, chroma: Double, tone: Double): Hct =
            Hct(((hue % 360.0) + 360.0) % 360.0, chroma.coerceAtLeast(0.0), tone.coerceIn(0.0, 100.0))

        /** CAM16 彩度低于此值视为无彩色，色相无意义。 */
        const val ACHROMATIC_CHROMA = 1.0

        /** 解出使 L* 等于目标的 CAM16 明度 J（J 单调，二分即可）。 */
        private fun solveJ(hue: Double, chroma: Double, tone: Double): Double {
            var low = 0.0
            var high = 100.0
            repeat(J_ITERATIONS) {
                val mid = (low + high) / 2.0
                if (lstarFromY(cam16ToXyz(hue, chroma, mid)[1]) < tone) low = mid else high = mid
            }
            return (low + high) / 2.0
        }
    }
}

// ── CAM16 ────────────────────────────────────────────────────────────────────

private class Cam16(
    val hue: Double,
    val chroma: Double,
    val j: Double
)

/** CAM16 观测条件，固定为 Material Color Utilities 的默认条件。 */
private class ViewingConditions(
    val n: Double,
    val aw: Double,
    val nbb: Double,
    val ncb: Double,
    val c: Double,
    val nc: Double,
    val rgbD: DoubleArray,
    val fl: DoubleArray,
    val z: Double
) {
    companion object {
        private val WHITE_POINT_D65 = doubleArrayOf(95.047, 100.0, 108.883)

        val DEFAULT: ViewingConditions = build(
            whitePoint = WHITE_POINT_D65,
            adaptingLuminance = 200.0 / PI * yFromLstar(50.0) / 100.0,
            backgroundLstar = 50.0,
            surround = 2.0
        )

        private fun build(
            whitePoint: DoubleArray,
            adaptingLuminance: Double,
            backgroundLstar: Double,
            surround: Double
        ): ViewingConditions {
            val rW = whitePoint[0] * M00 + whitePoint[1] * M01 + whitePoint[2] * M02
            val gW = whitePoint[0] * M10 + whitePoint[1] * M11 + whitePoint[2] * M12
            val bW = whitePoint[0] * M20 + whitePoint[1] * M21 + whitePoint[2] * M22

            val f = 0.8 + surround / 10.0
            val c = if (f >= 0.9) lerp(0.59, 0.69, (f - 0.9) * 10.0) else lerp(0.525, 0.59, (f - 0.8) * 10.0)
            val d = (f * (1.0 - (1.0 / 3.6) * kotlin.math.exp((-adaptingLuminance - 42.0) / 92.0))).coerceIn(0.0, 1.0)

            val rgbD = doubleArrayOf(
                d * (100.0 / rW) + 1.0 - d,
                d * (100.0 / gW) + 1.0 - d,
                d * (100.0 / bW) + 1.0 - d
            )

            val k = 1.0 / (5.0 * adaptingLuminance + 1.0)
            val k4 = k * k * k * k
            val fl = DoubleArray(3) {
                k4 * adaptingLuminance +
                    0.1 * (1.0 - k4) * (1.0 - k4) * (5.0 * adaptingLuminance).pow(1.0 / 3.0)
            }

            val n = yFromLstar(backgroundLstar) / whitePoint[1]
            val z = 1.48 + sqrt(n)
            val nbb = 0.725 / n.pow(0.2)

            val rgbAFactors = doubleArrayOf(
                (fl[0] * rgbD[0] * rW / 100.0).pow(0.42),
                (fl[1] * rgbD[1] * gW / 100.0).pow(0.42),
                (fl[2] * rgbD[2] * bW / 100.0).pow(0.42)
            )
            val rgbA = DoubleArray(3) { 400.0 * rgbAFactors[it] / (rgbAFactors[it] + 27.13) }
            val aw = (2.0 * rgbA[0] + rgbA[1] + 0.05 * rgbA[2]) * nbb

            return ViewingConditions(n, aw, nbb, nbb, c, f, rgbD, fl, z)
        }

        private fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t
    }
}

private const val M00 = 0.401288
private const val M01 = 0.650173
private const val M02 = -0.051461
private const val M10 = -0.250268
private const val M11 = 1.204414
private const val M12 = 0.045854
private const val M20 = -0.002079
private const val M21 = 0.048952
private const val M22 = 0.953127

private const val N00 = 1.8620678
private const val N01 = -1.0112547
private const val N02 = 0.14918678
private const val N10 = 0.38752654
private const val N11 = 0.62144744
private const val N12 = -0.00897398
private const val N20 = -0.01584150
private const val N21 = -0.03412294
private const val N22 = 1.0499644

private fun cam16FromXyz(x: Double, y: Double, z: Double): Cam16 {
    val vc = ViewingConditions.DEFAULT

    val rgb = doubleArrayOf(
        vc.rgbD[0] * (x * M00 + y * M01 + z * M02),
        vc.rgbD[1] * (x * M10 + y * M11 + z * M12),
        vc.rgbD[2] * (x * M20 + y * M21 + z * M22)
    )

    val rgbA = DoubleArray(3) { i ->
        val component = rgb[i]
        val p = (vc.fl[i] * abs(component) / 100.0).pow(0.42)
        sign(component) * 400.0 * p / (p + 27.13)
    }

    val a = (11.0 * rgbA[0] - 12.0 * rgbA[1] + rgbA[2]) / 11.0
    val b = (rgbA[0] + rgbA[1] - 2.0 * rgbA[2]) / 9.0
    val u = (20.0 * rgbA[0] + 20.0 * rgbA[1] + 21.0 * rgbA[2]) / 20.0
    val p2 = (40.0 * rgbA[0] + 20.0 * rgbA[1] + rgbA[2]) / 20.0

    val hueDegrees = atan2(b, a) * 180.0 / PI
    val hue = ((hueDegrees % 360.0) + 360.0) % 360.0
    val huePrime = if (hue < 20.36) hue + 360.0 else hue

    val ac = p2 * vc.nbb
    val j = 100.0 * (ac / vc.aw).pow(vc.c * vc.z)
    val eHue = 0.25 * (cos(huePrime * PI / 180.0 + 2.0) + 3.8)
    val p1 = 50000.0 / 13.0 * eHue * vc.nc * vc.ncb
    val t = p1 * kotlin.math.hypot(a, b) / (u + 0.305)
    val alpha = t.pow(0.9) * (1.64 - 0.29.pow(vc.n)).pow(0.73)
    val chroma = alpha * sqrt(j / 100.0)

    return Cam16(hue, chroma, j)
}

/** CAM16 → XYZ（D65，Y 归一到 0..100）。 */
private fun cam16ToXyz(hue: Double, chroma: Double, j: Double): DoubleArray {
    val vc = ViewingConditions.DEFAULT

    if (chroma < 1e-4 || j <= 0.0) {
        val y = yFromJ(j)
        return doubleArrayOf(y * 95.047 / 100.0, y, y * 108.883 / 100.0)
    }

    val alpha = chroma / sqrt(j / 100.0)
    val t = (alpha / (1.64 - 0.29.pow(vc.n)).pow(0.73)).pow(1.0 / 0.9)
    val hRad = hue * PI / 180.0

    val eHue = 0.25 * (cos(hRad + 2.0) + 3.8)
    val ac = vc.aw * (j / 100.0).pow(1.0 / vc.c / vc.z)
    val p1 = eHue * (50000.0 / 13.0) * vc.nc * vc.ncb
    val p2 = ac / vc.nbb

    val hSin = sin(hRad)
    val hCos = cos(hRad)
    val gamma = 23.0 * (p2 * t) / (23.0 * p1 + 11.0 * t * hCos + 108.0 * t * hSin)
    val a = gamma * hCos
    val b = gamma * hSin

    val rA = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0
    val gA = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0
    val bA = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0

    val rF = unadapt(rA, vc, 0)
    val gF = unadapt(gA, vc, 1)
    val bF = unadapt(bA, vc, 2)

    return doubleArrayOf(
        rF * N00 + gF * N01 + bF * N02,
        rF * N10 + gF * N11 + bF * N12,
        rF * N20 + gF * N21 + bF * N22
    )
}

private fun unadapt(componentA: Double, vc: ViewingConditions, index: Int): Double {
    val base = (27.13 * abs(componentA) / (400.0 - abs(componentA))).coerceAtLeast(0.0)
    val c = sign(componentA) * (100.0 / vc.fl[index]) * base.pow(1.0 / 0.42)
    return c / vc.rgbD[index]
}

/** J 与 Y 的近似对应（CAM16 明度 ↔ 相对亮度），用于无彩色路径。 */
private fun yFromJ(j: Double): Double = (j / 100.0).pow(1.0 / 0.7) * 100.0

// ── 明度与 sRGB 转换 ─────────────────────────────────────────────────────────

internal fun lstarFromY(y: Double): Double {
    val normalized = y / 100.0
    return if (normalized <= 216.0 / 24389.0) {
        normalized * 24389.0 / 27.0
    } else {
        normalized.pow(1.0 / 3.0) * 116.0 - 16.0
    }
}

internal fun yFromLstar(lstar: Double): Double =
    if (lstar > 8.0) ((lstar + 16.0) / 116.0).pow(3.0) * 100.0 else lstar / (24389.0 / 27.0) * 100.0

private fun argbToXyz(argb: Int): DoubleArray {
    val r = linearize(((argb shr 16) and 0xFF) / 255.0)
    val g = linearize(((argb shr 8) and 0xFF) / 255.0)
    val b = linearize((argb and 0xFF) / 255.0)
    return doubleArrayOf(
        r * 41.233895 + g * 35.762064 + b * 18.051042,
        r * 21.26 + g * 71.52 + b * 7.22,
        r * 1.932141 + g * 11.916382 + b * 95.034478
    )
}

internal fun colorFromXyz(xyz: DoubleArray): Color {
    val r = delinearize((xyz[0] * 3.2413775 - xyz[1] * 1.5376652 - xyz[2] * 0.49885367) / 100.0)
    val g = delinearize((-xyz[0] * 0.96914525 + xyz[1] * 1.8758853 + xyz[2] * 0.04156586) / 100.0)
    val b = delinearize((xyz[0] * 0.05562094 - xyz[1] * 0.20395525 + xyz[2] * 1.0571799) / 100.0)
    return Color(
        red = r.coerceIn(0.0, 1.0).toFloat(),
        green = g.coerceIn(0.0, 1.0).toFloat(),
        blue = b.coerceIn(0.0, 1.0).toFloat(),
        alpha = 1f
    )
}

private fun isInGamut(xyz: DoubleArray): Boolean {
    val r = (xyz[0] * 3.2413775 - xyz[1] * 1.5376652 - xyz[2] * 0.49885367) / 100.0
    val g = (-xyz[0] * 0.96914525 + xyz[1] * 1.8758853 + xyz[2] * 0.04156586) / 100.0
    val b = (xyz[0] * 0.05562094 - xyz[1] * 0.20395525 + xyz[2] * 1.0571799) / 100.0
    val tolerance = 1e-4
    return r >= -tolerance && r <= 1.0 + tolerance &&
        g >= -tolerance && g <= 1.0 + tolerance &&
        b >= -tolerance && b <= 1.0 + tolerance
}

/** 纯灰阶（无彩色）颜色，直接由 L* 生成。 */
internal fun colorFromLstar(lstar: Double): Color {
    val gray = delinearize(yFromLstar(lstar) / 100.0).coerceIn(0.0, 1.0).toFloat()
    return Color(red = gray, green = gray, blue = gray, alpha = 1f)
}

private fun linearize(component: Double): Double =
    if (component <= 0.04045) component / 12.92 else ((component + 0.055) / 1.055).pow(2.4)

private fun delinearize(component: Double): Double =
    if (component <= 0.0031308) 12.92 * component else 1.055 * component.pow(1.0 / 2.4) - 0.055
