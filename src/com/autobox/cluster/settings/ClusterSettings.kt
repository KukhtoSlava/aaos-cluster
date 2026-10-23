package com.autobox.cluster.settings

import com.autobox.cluster.model.GaugeStyle
import kotlinx.coroutines.flow.StateFlow

/**
 * Cluster UI preferences. They belong to the app, not to the vehicle, so they
 * are kept by the app rather than in the VHAL.
 */
interface ClusterSettings {
    val gaugeStyle: StateFlow<GaugeStyle>
    fun setGaugeStyle(style: GaugeStyle)
}
