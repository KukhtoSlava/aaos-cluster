package com.autobox.cluster.ui.map

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Rect
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.os.UserHandle
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceHolder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.autobox.navigation.cluster.IClusterMap
import com.autobox.navigation.cluster.IClusterMapListener

private const val TAG = "ClusterMapConnection"
private const val NAVIGATION_PACKAGE = "com.autobox.navigation"
private const val ACTION_CLUSTER_MAP = "com.autobox.navigation.action.CLUSTER_MAP"
private const val BIND_RETRY_MS = 2000L
private const val ATTACH_RETRY_MS = 5000L

/**
 * Hands the map surface to Navigation, which draws the cluster map into it
 * (com.autobox.navigation, IClusterMap).
 *
 * ClusterHomeService starts Cluster as the system user, while the driver's Navigation, with
 * the route and the car position, runs as the current foreground user. The binding therefore
 * goes to the current user and follows it on a user switch. Right after boot that user is
 * still locked and Navigation (not direct boot aware) cannot be found, so binding is retried
 * until it succeeds.
 */
internal class ClusterMapConnection(private val context: Context) : SurfaceHolder.Callback {

    /**
     * True once Navigation reports the map on the surface; until then [MapView] shows a
     * placeholder. Stays false if Navigation fails to draw, so the cluster never shows a dead
     * black surface.
     */
    var isMapShown by mutableStateOf(false)
        private set

    /** Density the map is drawn at, so 1 dp on the map is one design unit of the cluster. */
    var densityDpi = DisplayMetrics.DENSITY_DEFAULT

    /** Surface pixels at each edge covered by the gauges and bars. */
    var insets = Rect()

    private var connection: ServiceConnection? = null
    private var service: IClusterMap? = null
    private var holder: SurfaceHolder? = null
    private var width = 0
    private var height = 0

    // The listener of the attach in flight. Reports for an older surface compare unequal and
    // are dropped.
    private var attachListener: IClusterMapListener? = null

    private val handler = Handler(Looper.getMainLooper())
    private val retryBind = Runnable { bind() }

    private val userSwitchReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG, "User switched, binding to the new user's Navigation")
            rebind()
        }
    }

    fun start() {
        context.registerReceiver(
            userSwitchReceiver,
            IntentFilter(Intent.ACTION_USER_SWITCHED),
            Context.RECEIVER_NOT_EXPORTED,
        )
        bind()
    }

    fun stop() {
        context.unregisterReceiver(userSwitchReceiver)
        unbind()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        this.holder = holder
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        this.holder = holder
        this.width = width
        this.height = height
        attach()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        detach()
        this.holder = null
        width = 0
        height = 0
    }

    private fun bind() {
        val newConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                service = IClusterMap.Stub.asInterface(binder)
                attach()
            }

            // Navigation died; BIND_AUTO_CREATE restarts it and connects again.
            override fun onServiceDisconnected(name: ComponentName) {
                service = null
                attachListener = null
                isMapShown = false
            }

            // Navigation was updated or removed: this binding will never come back.
            override fun onBindingDied(name: ComponentName) {
                rebind()
            }
        }
        val intent = Intent(ACTION_CLUSTER_MAP).setPackage(NAVIGATION_PACKAGE)
        val user = UserHandle.of(ActivityManager.getCurrentUser())
        if (context.bindServiceAsUser(
                intent,
                newConnection,
                Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT,
                user,
            )
        ) {
            connection = newConnection
        } else {
            // A failed bind must still be released; older releases throw instead
            runCatching { context.unbindService(newConnection) }
            Log.i(TAG, "Navigation not available for $user yet, retrying")
            handler.postDelayed(retryBind, BIND_RETRY_MS)
        }
    }

    private fun unbind() {
        handler.removeCallbacks(retryBind)
        detach()
        connection?.let { context.unbindService(it) }
        connection = null
        service = null
    }

    private fun rebind() {
        unbind()
        bind()
    }

    private fun attach() {
        val map = service ?: return
        val surface = holder?.surface ?: return
        if (width == 0 || height == 0) return
        val listener = newAttachListener()
        attachListener = listener
        isMapShown = false
        try {
            map.attachSurface(surface, width, height, densityDpi, insets, listener)
        } catch (e: RemoteException) {
            attachListener = null
            Log.w(TAG, "Failed to attach the map surface", e)
        }
    }

    private fun detach() {
        if (attachListener == null) return
        attachListener = null
        isMapShown = false
        try {
            service?.detachSurface()
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to detach the map surface", e)
        }
    }

    private fun newAttachListener(): IClusterMapListener = object : IClusterMapListener.Stub() {
        override fun onMapShown() {
            handler.post { if (attachListener === this) isMapShown = true }
        }

        override fun onMapFailed(reason: String) {
            Log.w(TAG, "Navigation could not show the cluster map, retrying: $reason")
            handler.postDelayed({ if (attachListener === this) attach() }, ATTACH_RETRY_MS)
        }
    }
}
