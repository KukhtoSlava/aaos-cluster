package com.autobox.cluster.ui.gauges

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.autobox.cluster.R
import com.autobox.cluster.model.ClusterData
import java.util.Locale

/**
 * Range and graduation of a gauge, shared by the round and the arc layouts.
 * Every [majorEvery]-th tick is a major one. Ticks from [redFrom] and labels
 * from [labelRedFrom] are drawn in the redline colour.
 */
internal class GaugeScale(
    val max: Float,
    val ticks: Int,
    val labelStep: Int,
    val majorEvery: Int = 2,
    val redFrom: Float? = null,
    val labelRedFrom: Int? = null,
) {
    fun fraction(value: Float): Float = (value / max).coerceIn(0f, 1f)
    fun tickValue(i: Int): Float = max * i / ticks
    fun isMajor(i: Int): Boolean = i % majorEvery == 0
    fun isRedTick(value: Float): Boolean = redFrom != null && value >= redFrom
    fun isRedLabel(value: Int): Boolean = labelRedFrom != null && value >= labelRedFrom
    val labels: IntProgression get() = 0..max.toInt() step labelStep
}

internal val SpeedScale = GaugeScale(max = 220f, ticks = 22, labelStep = 20)
internal val RpmScale = GaugeScale(max = 8f, ticks = 16, labelStep = 1, redFrom = 6.5f, labelRedFrom = 7)

/** Speed value colour: amber above the current limit. */
@Composable
internal fun speedColor(data: ClusterData): Color {
    val limit = data.speedLimitKmh
    return if (limit != null && data.speedKmh > limit) {
        colorResource(R.color.cluster_speeding)
    } else {
        colorResource(R.color.cluster_text)
    }
}

/** Tachometer colour: accent, red from the redline. */
@Composable
internal fun rpmColor(data: ClusterData): Color =
    colorResource(if (RpmScale.isRedTick(data.rpmX1000)) R.color.cluster_redline else R.color.cluster_accent)

internal fun formatRpm(rpmX1000: Float): String = String.format(Locale.US, "%.1f", rpmX1000.coerceIn(0f, RpmScale.max))
