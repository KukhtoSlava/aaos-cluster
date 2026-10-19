package com.autobox.cluster.ui.gauges

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autobox.cluster.R
import com.autobox.cluster.model.ClusterData
import com.autobox.cluster.ui.components.ClusterText
import com.autobox.cluster.ui.components.CoolantBar
import com.autobox.cluster.ui.components.FuelBar
import com.autobox.cluster.ui.components.at
import com.autobox.cluster.ui.components.glowLine
import com.autobox.cluster.ui.theme.ClusterDimens
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val D = ClusterDimens.Arc

private const val VALUE_GLOW_ALPHA = 0.35f
private const val REDZONE_ALPHA = 0.35f

/**
 * One arc scale: an arc of radius D.Radius around (cx, D.CenterY) from [a0]
 * (zero) to [a1] (full scale). Both arcs fill from the bottom up.
 */
private class ArcScale(val gauge: GaugeScale, val cx: Dp, val a0: Float, val a1: Float) {
    val sweep get() = a1 - a0
    fun angle(fraction: Float) = a0 + sweep * fraction
    fun angleOf(value: Float) = angle(gauge.fraction(value))

    /** Point at [radius] from the circle centre, in design units. */
    fun point(angle: Float, radius: Dp): Offset {
        val a = Math.toRadians(angle.toDouble())
        return Offset(
            cx.value + radius.value * cos(a).toFloat(),
            D.CenterY.value + radius.value * sin(a).toFloat(),
        )
    }
}

// Speed on the left (circle centre right of it), RPM mirrored on the right.
private val SPEED = ArcScale(SpeedScale, D.SpeedCenterX, D.SPEED_START_ANGLE, D.SPEED_END_ANGLE)
private val RPM = ArcScale(RpmScale, D.RpmCenterX, D.RPM_START_ANGLE, D.RPM_END_ANGLE)

/** Speedometer and tachometer as arc scales on the screen edges, with large readouts. */
@Composable
internal fun ArcScalesLayer(data: ClusterData) {
    val accent = colorResource(R.color.cluster_accent)
    val rpmColor = rpmColor(data)
    val muted = colorResource(R.color.cluster_text_muted)

    Box(Modifier.fillMaxSize()) {
        ArcScalesCanvas(
            speedFraction = SpeedScale.fraction(data.speedKmh.toFloat()),
            rpmFraction = RpmScale.fraction(data.rpmX1000),
            speedColor = accent,
            rpmColor = rpmColor,
        )
        ArcLabels(SPEED)
        ArcLabels(RPM)

        // Left readout: speed, fuel
        Column(
            modifier = Modifier.at(D.LeftReadoutX, D.ReadoutY).width(D.ReadoutWidth),
            verticalArrangement = Arrangement.spacedBy(D.ReadoutGap),
        ) {
            Column {
                ClusterText(
                    data.speedKmh.toString(), D.BigValueSize, weight = FontWeight.SemiBold,
                    color = speedColor(data), letterSpacing = D.SpeedSpacing, lineHeight = D.BIG_VALUE_LINE_HEIGHT,
                )
                ClusterText(
                    stringResource(R.string.speed_unit), D.UnitSize, Modifier.padding(top = D.UnitGap),
                    color = muted, letterSpacing = D.UnitSpacing,
                )
            }
            FuelBar(data, width = D.LevelBarWidth, modifier = Modifier.padding(top = D.LevelBarGap))
        }

        // Right readout: RPM, coolant
        Column(
            modifier = Modifier.at(D.RightReadoutX, D.ReadoutY).width(D.ReadoutWidth),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(D.ReadoutGap),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                ClusterText(
                    formatRpm(data.rpmX1000), D.BigValueSize, weight = FontWeight.SemiBold,
                    color = colorResource(R.color.cluster_text), letterSpacing = D.RpmSpacing,
                    lineHeight = D.BIG_VALUE_LINE_HEIGHT,
                )
                ClusterText(
                    stringResource(R.string.rpm_unit), D.UnitSize, Modifier.padding(top = D.UnitGap),
                    color = muted, letterSpacing = D.UnitSpacing,
                )
            }
            CoolantBar(data, width = D.LevelBarWidth, modifier = Modifier.padding(top = D.LevelBarGap))
        }
    }
}

@Composable
private fun ArcScalesCanvas(speedFraction: Float, rpmFraction: Float, speedColor: Color, rpmColor: Color) {
    val track = colorResource(R.color.cluster_arc_track)
    val redline = colorResource(R.color.cluster_redline)
    val ground = colorResource(R.color.cluster_background)
    val major = colorResource(R.color.cluster_tick_major)
    val minor = colorResource(R.color.cluster_tick_minor)
    val marker = colorResource(R.color.cluster_marker)

    Canvas(Modifier.size(ClusterDimens.Width, ClusterDimens.Height)) {
        val u = 1.dp.toPx()

        drawScale(SPEED, speedFraction, speedColor, track, ground, redline)
        drawScale(RPM, rpmFraction, rpmColor, track, ground, redline)

        drawTicks(SPEED, major, minor, redline)
        drawTicks(RPM, major, minor, redline)

        listOf(SPEED to speedFraction, RPM to rpmFraction).forEachIndexed { i, (s, f) ->
            val a = s.angle(f)
            glowLine(
                color = marker,
                glow = if (i == 0) speedColor else rpmColor,
                start = s.point(a, D.Radius + D.MarkerOuter) * u,
                end = s.point(a, D.Radius - D.MarkerInner) * u,
                width = D.MarkerWidth.toPx(),
            )
        }
    }
}

private fun DrawScope.scalePath(s: ArcScale, from: Float, to: Float): Path {
    val u = 1.dp.toPx()
    return Path().apply {
        arcTo(
            rect = Rect(Offset(s.cx.value * u, D.CenterY.value * u), D.Radius.toPx()),
            startAngleDegrees = s.angle(from),
            sweepAngleDegrees = s.sweep * (to - from),
            forceMoveTo = true,
        )
    }
}

private fun DrawScope.drawScale(
    s: ArcScale,
    fraction: Float,
    color: Color,
    track: Color,
    ground: Color,
    redline: Color,
) {
    val f = fraction.coerceIn(0f, 1f)
    val band = Stroke(D.TrackWidth.toPx())
    drawPath(scalePath(s, 0f, 1f), track, style = band)
    s.gauge.redFrom?.let {
        drawPath(scalePath(s, s.gauge.fraction(it), 1f), redline.copy(alpha = REDZONE_ALPHA), style = band)
    }
    if (f > 0f) {
        val value = scalePath(s, 0f, f)
        drawPath(value, color.copy(alpha = VALUE_GLOW_ALPHA), style = Stroke(D.ValueGlowWidth.toPx()))
        drawPath(value, color, style = band)
    }
    // Cut the band into segments with the ground colour
    val percent = D.Radius.toPx() * Math.toRadians(abs(s.sweep).toDouble()).toFloat() / 100f
    drawPath(
        scalePath(s, 0f, 1f),
        ground,
        style = Stroke(
            width = D.SegmentCutWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(D.SEGMENT_CUT * percent, D.SEGMENT_GAP * percent),
                D.SEGMENT_PHASE * percent,
            ),
        ),
    )
}

private fun DrawScope.drawTicks(s: ArcScale, major: Color, minor: Color, redline: Color) {
    val u = 1.dp.toPx()
    val outer = D.Radius - D.TickInset
    for (i in 0..s.gauge.ticks) {
        val v = s.gauge.tickValue(i)
        val isMajor = s.gauge.isMajor(i)
        val a = s.angleOf(v)
        drawLine(
            color = when {
                s.gauge.isRedTick(v) -> redline
                isMajor -> major
                else -> minor
            },
            start = s.point(a, outer) * u,
            end = s.point(a, outer - if (isMajor) D.MajorTickLength else D.MinorTickLength) * u,
            strokeWidth = (if (isMajor) D.MajorTickWidth else D.MinorTickWidth).toPx(),
        )
    }
}

/** Scale labels centred on D.Radius - D.LabelInset. */
@Composable
private fun ArcLabels(s: ArcScale) {
    val normal = colorResource(R.color.cluster_tick_major)
    val red = colorResource(R.color.cluster_redline_label)
    for (v in s.gauge.labels) {
        val p = s.point(s.angleOf(v.toFloat()), D.Radius - D.LabelInset)
        Box(
            Modifier
                .at(p.x.dp - D.LabelBoxWidth / 2, p.y.dp - D.LabelBoxHeight / 2)
                .size(D.LabelBoxWidth, D.LabelBoxHeight),
            contentAlignment = Alignment.Center,
        ) {
            ClusterText(
                text = v.toString(),
                size = D.LabelSize,
                weight = FontWeight.Medium,
                color = if (s.gauge.isRedLabel(v)) red else normal,
            )
        }
    }
}
