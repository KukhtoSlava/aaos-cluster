package com.autobox.cluster.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import com.autobox.cluster.R
import com.autobox.cluster.ui.theme.ClusterDimens

/**
 * Full-bleed navigation map under the gauges. Empty for now: the map itself
 * is integrated in the next iteration.
 */
@Composable
internal fun MapView(modifier: Modifier = Modifier) {
    Box(modifier.background(colorResource(R.color.cluster_map)))
}

/** Fades the map into the ground under the top bar, the gauges and the bottom bar. */
@Composable
internal fun BoxScope.LegibilityFades() {
    val f = ClusterDimens.Fade
    val ground = colorResource(R.color.cluster_background)
    val clear = ground.copy(alpha = 0f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(f.TopHeight)
            .background(Brush.verticalGradient(0f to ground, f.TOP_SOLID to ground, 1f to clear)),
    )
    Box(
        Modifier
            .width(f.SideWidth)
            .fillMaxHeight()
            .background(Brush.horizontalGradient(0f to ground, f.SIDE_SOLID to ground, 1f to clear)),
    )
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .width(f.SideWidth)
            .fillMaxHeight()
            .background(Brush.horizontalGradient(0f to clear, 1f - f.SIDE_SOLID to ground, 1f to ground)),
    )
    Box(
        Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .height(f.BottomHeight)
            .background(Brush.verticalGradient(0f to clear, f.BOTTOM_SOLID to ground, 1f to ground)),
    )
}
