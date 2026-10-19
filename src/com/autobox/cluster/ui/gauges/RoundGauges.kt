package com.autobox.cluster.ui.gauges

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.autobox.cluster.R
import com.autobox.cluster.model.ClusterData
import com.autobox.cluster.ui.components.ClusterText
import com.autobox.cluster.ui.components.CoolantBar
import com.autobox.cluster.ui.components.FuelBar
import com.autobox.cluster.ui.components.at
import com.autobox.cluster.ui.components.glowLine
import com.autobox.cluster.ui.theme.ClusterDimens
import kotlin.math.cos
import kotlin.math.sin

private val D = ClusterDimens.Round

/** Angle from 12 o'clock, clockwise, of [fraction] of full scale. */
private fun dialAngle(fraction: Float): Float = D.START_ANGLE + D.SWEEP * fraction

/** Point at [radius] from the dial centre along a [dialAngle], in design units. */
private fun dialPoint(angle: Float, radius: Dp): Offset {
    val a = Math.toRadians(angle.toDouble())
    val c = D.Dial.value / 2f
    return Offset(c + radius.value * sin(a).toFloat(), c - radius.value * cos(a).toFloat())
}

/** Speedometer and tachometer as round dials. */
@Composable
internal fun RoundDialsLayer(data: ClusterData) {
    val accent = colorResource(R.color.cluster_accent)
    val rpmColor = rpmColor(data)

    Box(Modifier.fillMaxSize()) {
        // Speedometer
        RoundDial(
            modifier = Modifier.at(D.LeftX, D.DialY),
            value = data.speedKmh.toFloat(),
            scale = SpeedScale,
            labelSize = D.SpeedLabelSize,
            color = accent,
        ) {
            Readout(
                value = data.speedKmh.toString(),
                unit = stringResource(R.string.speed_unit),
                valueSize = D.SpeedValueSize,
                valueSpacing = D.SpeedValueSpacing,
                valueColor = speedColor(data),
                unitSize = D.SpeedUnitSize,
                unitGap = D.SpeedUnitGap,
                modifier = Modifier.at(D.ReadoutX, D.SpeedReadoutY),
            )
            FuelBar(data, width = D.LevelBarWidth, modifier = Modifier.at(D.LevelBarX, D.LevelBarY))
        }

        // Tachometer
        RoundDial(
            modifier = Modifier.at(D.RightX, D.DialY),
            value = data.rpmX1000,
            scale = RpmScale,
            labelSize = D.RpmLabelSize,
            color = rpmColor,
        ) {
            Readout(
                value = formatRpm(data.rpmX1000),
                unit = stringResource(R.string.rpm_unit),
                valueSize = D.RpmValueSize,
                valueSpacing = D.RpmValueSpacing,
                valueColor = colorResource(R.color.cluster_text),
                unitSize = D.RpmUnitSize,
                unitGap = D.RpmUnitGap,
                modifier = Modifier.at(D.ReadoutX, D.RpmReadoutY),
            )
            CoolantBar(data, width = D.LevelBarWidth, modifier = Modifier.at(D.LevelBarX, D.LevelBarY))
        }
    }
}

@Composable
private fun Readout(
    value: String,
    unit: String,
    valueSize: TextUnit,
    valueSpacing: TextUnit,
    valueColor: Color,
    unitSize: TextUnit,
    unitGap: Dp,
    modifier: Modifier,
) {
    Column(modifier = modifier.width(D.ReadoutWidth), horizontalAlignment = Alignment.CenterHorizontally) {
        ClusterText(
            text = value,
            size = valueSize,
            weight = FontWeight.SemiBold,
            color = valueColor,
            letterSpacing = valueSpacing,
            lineHeight = D.VALUE_LINE_HEIGHT,
            textAlign = TextAlign.Center,
        )
        ClusterText(
            text = unit,
            size = unitSize,
            modifier = Modifier.padding(top = unitGap),
            color = colorResource(R.color.cluster_text_muted),
            letterSpacing = D.UnitSpacing,
        )
    }
}

/**
 * One round dial. [content] is laid out in the dial's box, between the scale
 * and the needle (the needle passes over the readout, as in the design).
 */
@Composable
private fun RoundDial(
    value: Float,
    scale: GaugeScale,
    labelSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val face = colorResource(R.color.cluster_dial_face)
    val rim = colorResource(R.color.cluster_dial_rim)
    val track = colorResource(R.color.cluster_dial_track)
    val redline = colorResource(R.color.cluster_redline)
    val redLabel = colorResource(R.color.cluster_redline_label)
    val major = colorResource(R.color.cluster_tick_major)
    val minor = colorResource(R.color.cluster_tick_minor)
    val hub = colorResource(R.color.cluster_hub)
    val hubRim = colorResource(R.color.cluster_hub_rim)
    val fraction = scale.fraction(value)

    Box(modifier.size(D.Dial)) {
        Canvas(Modifier.fillMaxSize()) {
            val u = 1.dp.toPx()
            drawCircle(face, radius = D.FaceRadius.toPx(), center = center)
            drawCircle(rim, radius = D.RimRadius.toPx(), center = center, style = Stroke(D.RimWidth.toPx()))

            scale.redFrom?.let {
                dialArc(redline.copy(alpha = REDZONE_ALPHA), D.RedzoneRadius, scale.fraction(it), 1f, D.RedzoneWidth, StrokeCap.Butt)
            }
            dialArc(track, D.TrackRadius, 0f, 1f, D.TrackWidth, StrokeCap.Round)
            if (fraction > 0f) dialArc(color, D.TrackRadius, 0f, fraction, D.TrackWidth, StrokeCap.Round)

            for (i in 0..scale.ticks) {
                val v = scale.tickValue(i)
                val isMajor = scale.isMajor(i)
                val angle = dialAngle(scale.fraction(v))
                val length = if (isMajor) D.MajorTickLength else D.MinorTickLength
                drawLine(
                    color = when {
                        scale.isRedTick(v) -> redline
                        isMajor -> major
                        else -> minor
                    },
                    start = dialPoint(angle, D.TickRadius) * u,
                    end = dialPoint(angle, D.TickRadius - length) * u,
                    strokeWidth = (if (isMajor) D.MajorTickWidth else D.MinorTickWidth).toPx(),
                )
            }
        }

        // Labels centred on the label radius
        for (v in scale.labels) {
            val p = dialPoint(dialAngle(scale.fraction(v.toFloat())), D.LabelRadius)
            Box(
                Modifier
                    .at(p.x.dp - D.LabelBoxWidth / 2, p.y.dp - D.LabelBoxHeight / 2)
                    .size(D.LabelBoxWidth, D.LabelBoxHeight),
                contentAlignment = Alignment.Center,
            ) {
                ClusterText(
                    text = v.toString(),
                    size = labelSize,
                    weight = FontWeight.Medium,
                    color = if (scale.isRedLabel(v)) redLabel else major,
                )
            }
        }

        content()

        // Needle, then the hub over its pivot
        Canvas(Modifier.fillMaxSize()) {
            val u = 1.dp.toPx()
            val angle = dialAngle(fraction)
            glowLine(
                color = color,
                glow = color,
                start = dialPoint(angle, D.NeedleTip) * u,
                end = dialPoint(angle, -D.NeedleTail) * u,
                width = D.NeedleWidth.toPx(),
            )
            drawCircle(hub, radius = D.HubRadius.toPx(), center = center)
            // Rim stroke inside the hub edge, as CSS border with box-sizing: border-box
            drawCircle(
                hubRim,
                radius = (D.HubRadius - D.HubRimWidth / 2).toPx(),
                center = center,
                style = Stroke(D.HubRimWidth.toPx()),
            )
            drawCircle(color, radius = D.HubDotRadius.toPx(), center = center)
        }
    }
}

private const val REDZONE_ALPHA = 0.22f

/** Arc of the dial scale between two fractions of full scale. */
private fun DrawScope.dialArc(color: Color, radius: Dp, from: Float, to: Float, width: Dp, cap: StrokeCap) {
    val r = radius.toPx()
    drawArc(
        color = color,
        // dialAngle is measured from 12 o'clock, drawArc from 3 o'clock
        startAngle = dialAngle(from) - 90f,
        sweepAngle = D.SWEEP * (to - from),
        useCenter = false,
        topLeft = Offset(center.x - r, center.y - r),
        size = Size(2 * r, 2 * r),
        style = Stroke(width.toPx(), cap = cap),
    )
}
