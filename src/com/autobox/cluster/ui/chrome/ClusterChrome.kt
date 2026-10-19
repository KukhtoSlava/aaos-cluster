package com.autobox.cluster.ui.chrome

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.autobox.cluster.R
import com.autobox.cluster.model.ClusterData
import com.autobox.cluster.model.DriveMode
import com.autobox.cluster.model.Gear
import com.autobox.cluster.ui.components.ClockAndTemp
import com.autobox.cluster.ui.components.ClusterText
import com.autobox.cluster.ui.components.SpeedLimitSign
import com.autobox.cluster.ui.components.TelltaleRow
import com.autobox.cluster.ui.components.TurnSignal
import com.autobox.cluster.ui.components.at
import com.autobox.cluster.ui.components.leftTelltales
import com.autobox.cluster.ui.components.rightTelltales
import com.autobox.cluster.ui.theme.ClusterDimens

private val D = ClusterDimens.Chrome

/**
 * Everything that stays in place when the gauge style changes: the top bar,
 * the speed limit sign and the telltale rows. Only the gauges are swapped.
 */
@Composable
internal fun ClusterChrome(data: ClusterData, clock: String, flasherLit: Boolean) {
    val accent = colorResource(R.color.cluster_accent)

    Box(Modifier.fillMaxSize()) {
        // Top bar, left: turn signal and gear strip
        Row(
            modifier = Modifier.at(D.SideMargin, D.TopBarY).width(D.GroupWidth),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TurnSignal(left = true, active = data.turnSignalLeft, flasherLit = flasherLit)
            GearStrip(data.gear, accent)
        }
        // Top bar, right: clock and outside temperature, drive mode, turn signal
        Row(
            modifier = Modifier.at(D.RightX, D.TopBarY).width(D.GroupWidth),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClockAndTemp(clock, data.outsideTempC, timeSize = D.ClockSize, tempSize = D.TempSize)
            DriveModeLabel(data.driveMode, accent)
            TurnSignal(left = false, active = data.turnSignalRight, flasherLit = flasherLit)
        }

        data.speedLimitKmh?.let {
            SpeedLimitSign(
                it,
                size = D.LimitSize,
                ring = D.LimitRing,
                fontSize = D.LimitTextSize,
                modifier = Modifier.at(D.LimitX, D.LimitY),
            )
        }

        TelltaleRow(
            items = leftTelltales(data),
            iconSize = D.TelltaleSize,
            gap = D.TelltaleGap,
            arrangement = Alignment.CenterHorizontally,
            modifier = Modifier.at(D.SideMargin, D.TelltaleY).width(D.GroupWidth),
        )
        TelltaleRow(
            items = rightTelltales(data),
            iconSize = D.TelltaleSize,
            gap = D.TelltaleGap,
            arrangement = Alignment.CenterHorizontally,
            modifier = Modifier.at(D.RightX, D.TelltaleY).width(D.GroupWidth),
        )
    }
}

/** P R N D with the selected gear in the accent colour and underlined. */
@Composable
private fun GearStrip(current: Gear, accent: Color) {
    val inactive = colorResource(R.color.cluster_gear_inactive)
    Row(horizontalArrangement = Arrangement.spacedBy(D.GearGap)) {
        Gear.entries.forEach { g ->
            val selected = g == current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(D.GearUnderlineGap),
            ) {
                ClusterText(
                    g.label, D.GearSize, weight = FontWeight.SemiBold,
                    lineHeight = D.GEAR_LINE_HEIGHT, color = if (selected) accent else inactive,
                )
                Box(
                    Modifier
                        .size(D.GearUnderlineWidth, D.GearUnderlineHeight)
                        .background(
                            if (selected) accent else Color.Transparent,
                            RoundedCornerShape(D.GearUnderlineCorner),
                        ),
                )
            }
        }
    }
}

/** "MODE ECO", baseline-aligned. */
@Composable
private fun DriveModeLabel(mode: DriveMode, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(D.ModeGap)) {
        ClusterText(
            stringResource(R.string.drive_mode_label), D.ModeLabelSize, Modifier.alignByBaseline(),
            color = colorResource(R.color.cluster_text_muted), letterSpacing = D.ModeLabelSpacing,
        )
        ClusterText(
            stringResource(mode.labelRes()), D.ModeValueSize, Modifier.alignByBaseline(),
            weight = FontWeight.SemiBold, color = accent,
        )
    }
}

@StringRes
private fun DriveMode.labelRes(): Int = when (this) {
    DriveMode.ECO -> R.string.drive_mode_eco
    DriveMode.COMFORT -> R.string.drive_mode_comfort
    DriveMode.SPORT -> R.string.drive_mode_sport
}
