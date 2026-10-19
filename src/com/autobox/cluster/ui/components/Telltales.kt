package com.autobox.cluster.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import com.autobox.cluster.R
import com.autobox.cluster.model.ClusterData
import com.autobox.cluster.ui.theme.ClusterDimens
import kotlinx.coroutines.delay

private const val BLINK_HALF_PERIOD_MS = 450L
// A flashing turn signal dims to this alpha between flashes
private const val FLASHER_DIM_ALPHA = 0.12f

/**
 * Turn signal flasher phase. One flasher drives both arrows so that hazards
 * blink in sync: 450 ms lit, 450 ms dimmed.
 */
@Composable
internal fun rememberFlasher(active: Boolean): Boolean {
    var lit by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        lit = false
        if (!active) return@LaunchedEffect
        while (true) {
            lit = true
            delay(BLINK_HALF_PERIOD_MS)
            lit = false
            delay(BLINK_HALF_PERIOD_MS)
        }
    }
    return lit
}

@Composable
internal fun TurnSignal(left: Boolean, active: Boolean, flasherLit: Boolean, modifier: Modifier = Modifier) {
    val color = if (active) {
        colorResource(R.color.cluster_telltale_green).copy(alpha = if (flasherLit) 1f else FLASHER_DIM_ALPHA)
    } else {
        colorResource(R.color.cluster_telltale_off)
    }
    Icon(
        painter = painterResource(if (left) R.drawable.ic_turn_left else R.drawable.ic_turn_right),
        contentDescription = stringResource(if (left) R.string.telltale_turn_left else R.string.telltale_turn_right),
        tint = color,
        modifier = modifier.size(ClusterDimens.TurnSignal.Size),
    )
}

internal data class Telltale(
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
    val active: Boolean,
    val color: Color,
)

// Left group sits under the speedometer: lights, seat belt, engine
@Composable
internal fun leftTelltales(d: ClusterData): List<Telltale> {
    val green = colorResource(R.color.cluster_telltale_green)
    val blue = colorResource(R.color.cluster_telltale_blue)
    val amber = colorResource(R.color.cluster_telltale_amber)
    val red = colorResource(R.color.cluster_telltale_red)
    return listOf(
        Telltale(R.drawable.ic_tt_parking_lights, R.string.telltale_parking_lights, d.parkingLightsOn, green),
        Telltale(R.drawable.ic_tt_low_beam, R.string.telltale_low_beam, d.lowBeamOn, green),
        Telltale(R.drawable.ic_tt_high_beam, R.string.telltale_high_beam, d.highBeamOn, blue),
        Telltale(R.drawable.ic_tt_seatbelt, R.string.telltale_seatbelt, d.seatbeltWarning, red),
        Telltale(R.drawable.ic_tt_engine, R.string.telltale_engine, d.engineWarning, amber),
    )
}

// Right group sits under the tachometer: chassis, electrics, fluids
@Composable
internal fun rightTelltales(d: ClusterData): List<Telltale> {
    val amber = colorResource(R.color.cluster_telltale_amber)
    val red = colorResource(R.color.cluster_telltale_red)
    return listOf(
        Telltale(R.drawable.ic_tt_abs, R.string.telltale_abs, d.absActive, amber),
        Telltale(R.drawable.ic_tt_battery, R.string.telltale_battery, d.batteryWarning, red),
        Telltale(R.drawable.ic_tt_oil, R.string.telltale_oil, d.oilWarning, red),
        Telltale(R.drawable.ic_tt_parking_brake, R.string.telltale_brake, d.brakeWarning, red),
        Telltale(R.drawable.ic_tt_tpms, R.string.telltale_tpms, d.tpmsWarning, amber),
        Telltale(R.drawable.ic_fuel, R.string.telltale_fuel, d.fuelWarning, amber),
    )
}

/** A row of telltales with fixed slots: inactive ones stay visible, dimmed. */
@Composable
internal fun TelltaleRow(
    items: List<Telltale>,
    iconSize: Dp,
    gap: Dp,
    arrangement: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    val off = colorResource(R.color.cluster_telltale_off)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(gap, arrangement)) {
        items.forEach { t ->
            Icon(
                painter = painterResource(t.icon),
                contentDescription = stringResource(t.label),
                tint = if (t.active) t.color else off,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
