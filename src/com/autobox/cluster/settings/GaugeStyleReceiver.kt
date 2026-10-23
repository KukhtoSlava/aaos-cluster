package com.autobox.cluster.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.autobox.cluster.ClusterApplication
import com.autobox.cluster.model.GaugeStyle

/**
 * Switches the gauge style from adb, for test scripts:
 *
 *   adb shell am broadcast --user 0 -n com.autobox.cluster/.settings.GaugeStyleReceiver \
 *       -a com.autobox.cluster.action.SET_GAUGE_STYLE --es style arc
 *
 * The manifest guards it with android.permission.DUMP, which the shell holds.
 */
class GaugeStyleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SET_GAUGE_STYLE) return
        val value = intent.getStringExtra(EXTRA_STYLE)
        val style = GaugeStyle.fromPrefValue(value)
        if (style == null) {
            Log.w(TAG, "Unknown gauge style: $value")
            return
        }
        val app = context.applicationContext as ClusterApplication
        app.appComponent.clusterSettings().setGaugeStyle(style)
    }

    private companion object {
        const val TAG = "GaugeStyleReceiver"
        const val ACTION_SET_GAUGE_STYLE = "com.autobox.cluster.action.SET_GAUGE_STYLE"
        const val EXTRA_STYLE = "style"
    }
}
