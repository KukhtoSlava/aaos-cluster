package com.autobox.cluster.settings.impl

import android.content.Context
import android.content.SharedPreferences
import com.autobox.cluster.model.GaugeStyle
import com.autobox.cluster.settings.ClusterSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [ClusterSettings] in device-protected SharedPreferences. */
@Singleton
class ClusterSettingsImpl @Inject constructor(context: Context) : ClusterSettings {

    // Device-protected storage: the cluster is direct-boot aware and starts
    // before the user unlocks.
    private val prefs = context.createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _gaugeStyle = MutableStateFlow(readGaugeStyle())
    override val gaugeStyle: StateFlow<GaugeStyle> = _gaugeStyle.asStateFlow()

    // Picks up writes from anywhere in the process, GaugeStyleReceiver included.
    // Kept in a field: SharedPreferences holds listeners weakly.
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_GAUGE_STYLE) _gaugeStyle.value = readGaugeStyle()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun setGaugeStyle(style: GaugeStyle) {
        prefs.edit().putString(KEY_GAUGE_STYLE, style.prefValue).apply()
    }

    private fun readGaugeStyle(): GaugeStyle =
        GaugeStyle.fromPrefValue(prefs.getString(KEY_GAUGE_STYLE, null)) ?: GaugeStyle.ROUND

    private companion object {
        const val PREFS_NAME = "cluster_settings"
        const val KEY_GAUGE_STYLE = "gauge_style"
    }
}
