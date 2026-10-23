package com.autobox.cluster.ui.bottombar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.autobox.cluster.R
import com.autobox.cluster.model.GaugeStyle
import com.autobox.cluster.ui.components.ClusterText
import com.autobox.cluster.ui.theme.ClusterDimens
import com.autobox.cluster.ui.theme.CssEase

private const val SWITCH_FADE_MS = 250

/** Odometer and gauge style switcher, centred along the bottom edge. */
@Composable
internal fun BottomBar(
    odometerKm: Int,
    gaugeStyle: GaugeStyle,
    onSelectGaugeStyle: (GaugeStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            ClusterDimens.BottomBar.Gap,
            Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OdometerPill(odometerKm)
        GaugeStyleSwitcher(gaugeStyle, onSelectGaugeStyle)
    }
}

@Composable
private fun Modifier.panel(): Modifier {
    val shape = RoundedCornerShape(ClusterDimens.Panel.Corner)
    return this
        .background(colorResource(R.color.cluster_panel), shape)
        .border(ClusterDimens.Panel.Border, colorResource(R.color.cluster_panel_border), shape)
}

@Composable
private fun OdometerPill(odometerKm: Int) {
    val b = ClusterDimens.BottomBar
    val secondary = colorResource(R.color.cluster_text_secondary)
    Row(
        modifier = Modifier
            .panel()
            .padding(horizontal = b.OdoPaddingH, vertical = b.OdoPaddingV),
        horizontalArrangement = Arrangement.spacedBy(b.OdoGap),
    ) {
        ClusterText(
            stringResource(R.string.odo_label), b.OdoLabelSize, Modifier.alignByBaseline(),
            color = secondary, letterSpacing = b.OdoLabelSpacing,
        )
        ClusterText(odometerKm.toString(), b.OdoValueSize, Modifier.alignByBaseline(), weight = FontWeight.SemiBold)
        ClusterText(stringResource(R.string.odo_unit), b.OdoLabelSize, Modifier.alignByBaseline(), color = secondary)
    }
}

@Composable
private fun GaugeStyleSwitcher(current: GaugeStyle, onSelect: (GaugeStyle) -> Unit) {
    Row(
        modifier = Modifier
            .panel()
            .padding(ClusterDimens.BottomBar.SwitcherPadding)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ClusterDimens.BottomBar.SwitcherGap),
    ) {
        SwitcherButton(GaugeStyle.ROUND, R.drawable.ic_view_round, R.string.gauge_style_round, current, onSelect)
        SwitcherButton(GaugeStyle.ARC, R.drawable.ic_view_arc, R.string.gauge_style_arc, current, onSelect)
    }
}

@Composable
private fun SwitcherButton(
    style: GaugeStyle,
    @DrawableRes icon: Int,
    @StringRes label: Int,
    current: GaugeStyle,
    onSelect: (GaugeStyle) -> Unit,
) {
    val b = ClusterDimens.BottomBar
    val selected = style == current
    val selectedBackground = colorResource(R.color.cluster_switch_selected)
    val background by animateColorAsState(
        targetValue = if (selected) selectedBackground else selectedBackground.copy(alpha = 0f),
        animationSpec = tween(SWITCH_FADE_MS, easing = CssEase),
        label = "switcherBackground",
    )
    val tint by animateColorAsState(
        targetValue = colorResource(if (selected) R.color.cluster_accent else R.color.cluster_text_muted),
        animationSpec = tween(SWITCH_FADE_MS, easing = CssEase),
        label = "switcherTint",
    )
    Box(
        modifier = Modifier
            .size(b.ButtonWidth, b.ButtonHeight)
            .clip(RoundedCornerShape(b.ButtonCorner))
            .background(background)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(style) }),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(label),
            tint = tint,
            modifier = Modifier.size(b.ButtonIconSize),
        )
    }
}
