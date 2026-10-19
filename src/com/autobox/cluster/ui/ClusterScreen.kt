package com.autobox.cluster.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.autobox.cluster.R
import com.autobox.cluster.ui.bottombar.BottomBar
import com.autobox.cluster.ui.chrome.ClusterChrome
import com.autobox.cluster.ui.components.at
import com.autobox.cluster.ui.components.rememberClockText
import com.autobox.cluster.ui.components.rememberFlasher
import com.autobox.cluster.ui.gauges.ArcScalesLayer
import com.autobox.cluster.ui.gauges.RoundDialsLayer
import com.autobox.cluster.ui.map.LegibilityFades
import com.autobox.cluster.ui.map.MapView
import com.autobox.cluster.ui.theme.ClusterDimens
import com.autobox.cluster.ui.theme.CssEase
import kotlin.math.min

private const val LAYER_FADE_MS = 450
private const val HIDDEN_LAYER_SCALE = 0.97f

@Composable
fun ClusterScreen(viewModel: ClusterViewModel) {
    val data by viewModel.data.collectAsState()
    val gaugeStyle by viewModel.gaugeStyle.collectAsState()
    val clock = rememberClockText()
    val flasherLit = rememberFlasher(data.turnSignalLeft || data.turnSignalRight)

    ScaledCanvas {
        MapView(Modifier.fillMaxSize())
        LegibilityFades()

        // Only the gauges change with the style; the chrome around them stays put
        GaugeLayer(visible = gaugeStyle == GaugeStyle.ROUND) {
            RoundDialsLayer(data)
        }
        GaugeLayer(visible = gaugeStyle == GaugeStyle.ARC) {
            ArcScalesLayer(data)
        }
        ClusterChrome(data, clock, flasherLit)

        BottomBar(
            odometerKm = data.odometerKm,
            gaugeStyle = gaugeStyle,
            onSelectGaugeStyle = viewModel::setGaugeStyle,
            modifier = Modifier.at(0.dp, ClusterDimens.BottomBar.Y).fillMaxWidth(),
        )
    }
}

/**
 * Lays out [content] on a fixed 1848x792 canvas (the emulator cluster display)
 * where 1.dp is one design unit, scaled uniformly to fit the actual display.
 */
@Composable
private fun ScaledCanvas(content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.cluster_background)),
        contentAlignment = Alignment.Center,
    ) {
        val scale = min(
            constraints.maxWidth / ClusterDimens.Width.value,
            constraints.maxHeight / ClusterDimens.Height.value,
        )
        CompositionLocalProvider(LocalDensity provides Density(density = scale, fontScale = 1f)) {
            Box(
                modifier = Modifier
                    .size(ClusterDimens.Width, ClusterDimens.Height)
                    .clipToBounds(),
                content = content,
            )
        }
    }
}

/** Cross-fades and slightly zooms a gauge layout in and out. */
@Composable
private fun GaugeLayer(visible: Boolean, content: @Composable BoxScope.() -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(LAYER_FADE_MS, easing = CssEase),
        label = "gaugeLayer",
    )
    if (progress == 0f) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = progress
                val s = HIDDEN_LAYER_SCALE + (1f - HIDDEN_LAYER_SCALE) * progress
                scaleX = s
                scaleY = s
            },
        content = content,
    )
}
