package com.autobox.cluster.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import com.autobox.cluster.R
import com.autobox.cluster.model.ClusterData
import com.autobox.cluster.ui.theme.ClusterDimens

private const val COOLANT_MIN_C = 40f
private const val COOLANT_MAX_C = 130f

/** Small level gauge: icon, thin bar, value text. Used for fuel and coolant. */
@Composable
internal fun LevelBar(
    @DrawableRes icon: Int,
    label: String,
    fraction: Float,
    text: String,
    fill: Color,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val dimens = ClusterDimens.LevelBar
    val shape = RoundedCornerShape(dimens.BarCorner)
    Row(
        modifier = modifier.width(width),
        horizontalArrangement = Arrangement.spacedBy(dimens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = colorResource(R.color.cluster_text_muted),
            modifier = Modifier.size(dimens.IconSize),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(dimens.BarHeight)
                .clip(shape)
                .background(colorResource(R.color.cluster_bar_track)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(fill, shape),
            )
        }
        ClusterText(text, dimens.TextSize, color = colorResource(R.color.cluster_text_secondary))
    }
}

@Composable
internal fun FuelBar(data: ClusterData, width: Dp, modifier: Modifier = Modifier) {
    LevelBar(
        icon = R.drawable.ic_fuel,
        label = stringResource(R.string.fuel_label),
        fraction = data.fuelPercent / 100f,
        text = data.rangeKm?.let { stringResource(R.string.range_format, it) }
            ?: stringResource(R.string.range_unknown),
        fill = if (data.fuelWarning) colorResource(R.color.cluster_telltale_amber) else colorResource(R.color.cluster_bar_fill),
        width = width,
        modifier = modifier,
    )
}

@Composable
internal fun CoolantBar(data: ClusterData, width: Dp, modifier: Modifier = Modifier) {
    LevelBar(
        icon = R.drawable.ic_coolant,
        label = stringResource(R.string.coolant_label),
        fraction = (data.coolantTempC - COOLANT_MIN_C) / (COOLANT_MAX_C - COOLANT_MIN_C),
        text = stringResource(R.string.coolant_format, data.coolantTempC),
        fill = if (data.engineWarning) colorResource(R.color.cluster_telltale_red) else colorResource(R.color.cluster_bar_fill),
        width = width,
        modifier = modifier,
    )
}
