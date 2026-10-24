package com.autobox.cluster.ui.map

import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.viewinterop.AndroidView
import com.autobox.cluster.R
import com.autobox.cluster.ui.theme.ClusterDimens
import kotlin.math.roundToInt

/**
 * Full-bleed navigation map under the gauges, drawn by the Navigation app into this
 * SurfaceView (see [ClusterMapConnection]). The plain map colour stands in until
 * Navigation is drawing.
 */
@Composable
internal fun MapView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val connection = remember { ClusterMapConnection(context) }
    DisposableEffect(connection) {
        connection.start()
        onDispose { connection.stop() }
    }

    // ScaledCanvas makes 1.dp one design unit, so this density draws the map's own UI in
    // design units too, and the insets come out in surface pixels.
    val density = LocalDensity.current
    SideEffect {
        val m = ClusterDimens.Map
        with(density) {
            connection.densityDpi = (DisplayMetrics.DENSITY_DEFAULT * density.density).roundToInt()
            connection.insets = Rect(
                m.InsetSide.roundToPx(),
                m.InsetTop.roundToPx(),
                m.InsetSide.roundToPx(),
                m.InsetBottom.roundToPx(),
            )
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { SurfaceView(it).apply { holder.addCallback(connection) } },
            modifier = Modifier.fillMaxSize(),
        )
        if (!connection.isMapShown) {
            Box(Modifier.fillMaxSize().background(colorResource(R.color.cluster_map)))
        }
    }
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
