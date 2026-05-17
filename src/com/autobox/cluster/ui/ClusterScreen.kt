package com.autobox.cluster.ui

import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobox.cluster.R
import com.autobox.cluster.model.ClusterData
import com.autobox.cluster.model.Gear
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val GAUGE_START_ANGLE = 135f
private const val GAUGE_SWEEP = 270f
private const val LOW_FUEL_PERCENT = 15
private const val MIN_TEMP_C = 35
private const val LOW_TEMP_C = 50
private const val HIGH_TEMP_C = 90
private const val MAX_TEMP_C = 120

// Gauge arc starts at 7 o'clock and sweeps clockwise to 5 o'clock (gap at bottom)

@Composable
fun ClusterScreen(viewModel: ClusterViewModel) {
    val data by viewModel.data.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar()
            MainPanel(data = data, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun TopBar() {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.header_height))
            .background(cs.surface)
            .padding(horizontal = dimensionResource(R.dimen.padding_xxl)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs))) {
            Box(modifier = Modifier.size(dimensionResource(R.dimen.header_dot_size)).background(cs.primary, CircleShape))
            Text(stringResource(R.string.app_name).uppercase(), color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_header).value.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// 5-column layout: [warnings left | speed | center info | rpm | warnings right]
@Composable
private fun MainPanel(data: ClusterData, modifier: Modifier) {
    Row(modifier = modifier) {
        LeftWarnings(data = data, modifier = Modifier.weight(0.32f).fillMaxHeight())
        SpeedSection(data = data, modifier = Modifier.weight(1f).fillMaxHeight())
        CenterPanel(data = data, modifier = Modifier.weight(0.5f).fillMaxHeight())
        RpmSection(data = data, modifier = Modifier.weight(1f).fillMaxHeight())
        RightWarnings(data = data, modifier = Modifier.weight(0.32f).fillMaxHeight())
    }
}

// Left panel: ABS / ENG / BAT / OIL — fixed positions, never hidden
@Composable
private fun LeftWarnings(data: ClusterData, modifier: Modifier) {
    val red = colorResource(R.color.cluster_red)
    val amber = colorResource(R.color.cluster_amber)
    Column(
        modifier = modifier.padding(dimensionResource(R.dimen.padding_m)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_l)),
    ) {
        Indicator(R.drawable.ic_warning_abs,     stringResource(R.string.telltale_abs),     data.absActive,      red)
        Indicator(R.drawable.ic_warning_engine,  stringResource(R.string.telltale_engine),  data.engineWarning,  amber)
        Indicator(R.drawable.ic_warning_battery, stringResource(R.string.telltale_battery), data.batteryWarning, amber)
        Indicator(R.drawable.ic_warning_oil,     stringResource(R.string.telltale_oil),     data.oilWarning,     red)
    }
}

// Right panel: BELT / TPMS / FUEL / BRAKE — fixed positions, never hidden
@Composable
private fun RightWarnings(data: ClusterData, modifier: Modifier) {
    val red = colorResource(R.color.cluster_red)
    val amber = colorResource(R.color.cluster_amber)
    Column(
        modifier = modifier.padding(dimensionResource(R.dimen.padding_m)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_l)),
    ) {
        Indicator(R.drawable.ic_warning_seatbelt, stringResource(R.string.telltale_seatbelt), data.seatbeltWarning, red)
        Indicator(R.drawable.ic_warning_tpms,     stringResource(R.string.telltale_tpms),     data.tpmsWarning,     amber)
        Indicator(R.drawable.ic_warning_fuel,     stringResource(R.string.telltale_fuel),     data.fuelWarning,     amber)
        Indicator(R.drawable.ic_warning_brake,    stringResource(R.string.telltale_brake),    data.brakeWarning,    red)
    }
}

// Speed gauge — centered in its column
@Composable
private fun SpeedSection(data: ClusterData, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val purple = colorResource(R.color.cluster_purple)
    val fraction = (data.speedKmh / 260f).coerceIn(0f, 1f)
    val gaugeColor = when {
        fraction < 0.3f -> purple
        fraction < 0.7f -> cs.secondary
        else            -> cs.error
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularGauge(
            modifier = Modifier.size(dimensionResource(R.dimen.gauge_size)),
            value = fraction,
            displayValue = data.speedKmh.toString(),
            unit = stringResource(R.string.speed_unit),
            gaugeColor = gaugeColor,
            tickLabels = listOf("0", "50", "100", "150", "200", "250"),
            tickFractions = listOf(0f, 50f / 260f, 100f / 260f, 150f / 260f, 200f / 260f, 250f / 260f),
        )
    }
}

// RPM gauge — centered in its column
@Composable
private fun RpmSection(data: ClusterData, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val fraction = (data.rpmX1000 / 8f).coerceIn(0f, 1f)
    val gaugeColor = if (fraction > 0.6f) cs.error else cs.tertiary
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularGauge(
            modifier = Modifier.size(dimensionResource(R.dimen.gauge_size)),
            value = fraction,
            displayValue = String.format("%.1f", data.rpmX1000),
            unit = stringResource(R.string.rpm_unit),
            gaugeColor = gaugeColor,
            tickLabels = listOf("0", "2", "4", "6", "8"),
            tickFractions = listOf(0f, 2f / 8f, 4f / 8f, 6f / 8f, 8f / 8f),
        )
    }
}

// Center: lights row | big gear letter | fuel/temp bars
@Composable
private fun CenterPanel(data: ClusterData, modifier: Modifier) {
    val cs    = MaterialTheme.colorScheme
    val green = colorResource(R.color.cluster_green)
    val blue = colorResource(R.color.cluster_blue)
    val red = colorResource(R.color.cluster_red)
    val amber = colorResource(R.color.cluster_amber)
    val fuelColor = if (data.fuelPercent <= LOW_FUEL_PERCENT) red else green
    val tempColor = when {
        data.coolantTempC <= LOW_TEMP_C -> blue
        data.coolantTempC >= HIGH_TEMP_C -> red
        else -> green
    }
    val tempValue = ((data.coolantTempC - MIN_TEMP_C).toFloat() / (MAX_TEMP_C - MIN_TEMP_C))
        .coerceIn(0f, 1f)
    val gearColor = when (data.gear) {
        Gear.P -> green
        Gear.R -> red
        Gear.N -> amber
        Gear.D -> blue
    }
    Column(
        modifier = modifier.padding(
            horizontal = dimensionResource(R.dimen.padding_s),
            vertical = dimensionResource(R.dimen.padding_l),
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // ← HIGH  PARK  LOW →  — evenly distributed across full width
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TurnSignal(R.drawable.ic_turn_left,   stringResource(R.string.telltale_turn_left),  data.turnSignalLeft,   green)
            Indicator(R.drawable.ic_light_high_beam, stringResource(R.string.telltale_high_beam), data.highBeamOn,    blue)
            Indicator(R.drawable.ic_parking_lights,  stringResource(R.string.telltale_parking_lights), data.parkingLightsOn,green)
            Indicator(R.drawable.ic_light_dimmed,    stringResource(R.string.telltale_low_beam),  data.lowBeamOn,     green)
            TurnSignal(R.drawable.ic_turn_right,  stringResource(R.string.telltale_turn_right), data.turnSignalRight,  green)
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_m)))
        Text(
            text = data.gear.label,
            color = gearColor,
            fontSize = dimensionResource(R.dimen.text_gear_display).value.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_l)))
        BarIndicator(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.fuel_label),
            value = data.fuelPercent / 100f,
            displayText = stringResource(R.string.fuel_value_format, data.fuelPercent),
            color = fuelColor,
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_m)))
        BarIndicator(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.temp_label),
            value = tempValue,
            displayText = stringResource(R.string.temp_value_format, data.coolantTempC),
            color = tempColor,
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_s)))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.odo_format, data.odometerKm), color = cs.onSurface, fontSize = dimensionResource(R.dimen.text_bar_label).value.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_xs)))
            Text(stringResource(R.string.odo_unit), color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_bar_label).value.sp)
        }
    }
}

@Composable
private fun BarIndicator(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
    displayText: String,
    color: Color,
) {
    val cs = MaterialTheme.colorScheme
    val radius = RoundedCornerShape(dimensionResource(R.dimen.radius_xs))
    val clampedValue = value.coerceIn(0f, 1f)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                color = cs.onSurfaceVariant,
                fontSize = dimensionResource(R.dimen.text_bar_label).value.sp,
                letterSpacing = 1.sp,
            )
            Text(
                text = displayText,
                color = color,
                fontSize = dimensionResource(R.dimen.text_bar_label).value.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.bar_height))
                .background(cs.surface, radius),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedValue)
                    .fillMaxHeight()
                    .background(color, radius),
            )
        }
    }
}

@Composable
private fun CircularGauge(
    modifier: Modifier,
    value: Float,
    displayValue: String,
    unit: String,
    gaugeColor: Color,
    tickLabels: List<String>,
    tickFractions: List<Float>,
) {
    val cs = MaterialTheme.colorScheme
    val strokeWidthDp = dimensionResource(R.dimen.gauge_stroke)
    val tickColor = colorResource(R.color.cluster_gauge_tick)
    val hubColor = colorResource(R.color.cluster_gauge_hub)
    val white = colorResource(R.color.cluster_white)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidthDp.toPx()
            val inset = strokePx / 2f
            val arcTopLeft = Offset(inset, inset)
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.width / 2f - inset

            // Track (full arc, dark)
            drawArc(
                color = cs.surface,
                startAngle = GAUGE_START_ANGLE,
                sweepAngle = GAUGE_SWEEP,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // Value arc
            if (value > 0.001f) {
                drawArc(
                    color = gaugeColor,
                    startAngle = GAUGE_START_ANGLE,
                    sweepAngle = GAUGE_SWEEP * value,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }

            // Tick marks — short lines at label positions
            tickFractions.forEach { fraction ->
                val angleDeg = GAUGE_START_ANGLE + GAUGE_SWEEP * fraction
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val outerR = radius
                val innerR = radius - strokePx * 1.3f
                val lit = fraction <= value
                drawLine(
                    color = if (lit) gaugeColor.copy(alpha = 0.7f) else tickColor,
                    start = Offset(
                        cx + (innerR * cos(angleRad)).toFloat(),
                        cy + (innerR * sin(angleRad)).toFloat(),
                    ),
                    end = Offset(
                        cx + (outerR * cos(angleRad)).toFloat(),
                        cy + (outerR * sin(angleRad)).toFloat(),
                    ),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }

            // Tick labels via native canvas (Compose text can't be drawn inside Canvas drawScope)
            val labelPaint = AndroidPaint().apply {
                color = tickColor.toArgb()
                textSize = 10.dp.toPx()
                textAlign = AndroidPaint.Align.CENTER
                isAntiAlias = true
            }
            val labelRadius = radius - strokePx * 2.4f
            tickFractions.forEachIndexed { i, fraction ->
                val angleDeg = GAUGE_START_ANGLE + GAUGE_SWEEP * fraction
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val lx = cx + (labelRadius * cos(angleRad)).toFloat()
                val ly = cy + (labelRadius * sin(angleRad)).toFloat()
                drawContext.canvas.nativeCanvas.drawText(
                    tickLabels[i],
                    lx,
                    ly + labelPaint.textSize / 3f,
                    labelPaint,
                )
            }

            // Needle — white line from center to inner arc edge
            val needleAngleRad = Math.toRadians((GAUGE_START_ANGLE + GAUGE_SWEEP * value).toDouble())
            val needleLen = radius - strokePx * 0.6f
            drawLine(
                color = white.copy(alpha = 0.9f),
                start = Offset(cx, cy),
                end = Offset(
                    cx + (needleLen * cos(needleAngleRad)).toFloat(),
                    cy + (needleLen * sin(needleAngleRad)).toFloat(),
                ),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )

            // Center hub
            drawCircle(color = hubColor, radius = strokePx * 0.75f, center = Offset(cx, cy))
            drawCircle(color = white.copy(alpha = 0.5f), radius = strokePx * 0.4f, center = Offset(cx, cy))
        }

        // Value text overlaid on canvas — nudge upward so it sits in the visual arc center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.gauge_value_bottom_offset)),
        ) {
            Text(
                text = displayValue,
                color = white,
                fontSize = dimensionResource(R.dimen.text_gauge_value).value.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
            )
            Text(
                text = unit,
                color = cs.onSurfaceVariant,
                fontSize = dimensionResource(R.dimen.text_gauge_unit).value.sp,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}


@Composable
private fun Indicator(iconRes: Int, label: String, active: Boolean, activeColor: Color) {
    val dimIconColor = colorResource(R.color.cluster_dim_icon)
    val color = if (active) activeColor else dimIconColor
    Icon(
        painter = painterResource(iconRes),
        contentDescription = label,
        tint = color,
        modifier = Modifier.size(dimensionResource(R.dimen.telltale_icon_size)),
    )
}

@Composable
private fun TurnSignal(iconRes: Int, label: String, active: Boolean, color: Color) {
    val dimIconColor = colorResource(R.color.cluster_dim_icon)
    var lit by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (active) {
            while (true) {
                lit = true
                delay(500L)
                lit = false
                delay(500L)
            }
        } else {
            lit = false
        }
    }
    Icon(
        painter = painterResource(iconRes),
        contentDescription = label,
        tint = if (lit) color else dimIconColor,
        modifier = Modifier.size(dimensionResource(R.dimen.turn_signal_size)),
    )
}
