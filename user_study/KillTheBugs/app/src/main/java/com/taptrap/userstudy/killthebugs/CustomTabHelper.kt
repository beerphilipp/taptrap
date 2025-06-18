package com.taptrap.userstudy.killthebugs

import android.content.Context
import android.graphics.Color
import android.net.Uri
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import android.util.Log

/**
 * Helper class to manage Custom Tab connections.
 */
class CustomTabHelper(private val context: Context, private val clickListener: ClickListener?) {

    private var customTabsClient: CustomTabsClient? = null
    private var customTabsSession: CustomTabsSession? = null
    private var startedCount = 0
    private lateinit var rawURL: String

    /**
     * Service connection to bind to the Custom Tabs service.
     */
    private val customTabsServiceConnection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: android.content.ComponentName, client: CustomTabsClient) {
            customTabsClient = client
            customTabsClient?.warmup(0L) // Preload resources
            customTabsSession = customTabsClient?.newSession(customTabsCallback)
            customTabsSession?.mayLaunchUrl(Uri.parse(rawURL), null, null)
        }

        override fun onServiceDisconnected(name: android.content.ComponentName) {
            customTabsClient = null
            customTabsSession = null
        }
    }

    init {
        rawURL = context.getString(R.string.webapp)
        // Bind to Custom Tabs service
        CustomTabsClient.bindCustomTabsService(context, "com.android.chrome", customTabsServiceConnection)
    }

    // Callback to monitor URL changes
    private val customTabsCallback = object : CustomTabsCallback() {
        override fun onNavigationEvent(navigationEvent: Int, extras: android.os.Bundle?) {
            when (navigationEvent) {
                NAVIGATION_STARTED -> {
                    Log.d("CustomTabs", "Navigation started")
                    startedCount++
                    if (startedCount == 2) {
                        Log.i("CustomTabs", "Click registered.")
                        clickListener?.clicked(true)
                    }
                }
                NAVIGATION_FINISHED -> Log.d("CustomTabs", "Navigation finished")
                NAVIGATION_FAILED -> Log.d("CustomTabs", "Navigation failed")
            }
        }
    }

    /**
     * Opens the Custom Tab to show the how-to page.
     * @param url The URL to open in the Custom Tab.
     */
    fun openCustomTabHowTo(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder(customTabsSession)
            .setShowTitle(true)
            .setToolbarColor(Color.parseColor("#6200EA"))
            .setStartAnimations(context, 0, 0)
            .setExitAnimations(context, 0, 0)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setUrlBarHidingEnabled(false)
            .build()

        // Launch URL in Custom Tab
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }

    /**
     * Launches a Custom Tab with the specified URL.
     * @param url The URL to open in the Custom Tab.
     * @param mode one of 'LOCATION', 'LOCATION_ADMIN', 'CAMERA', 'CAMERA_ADMIN'.
     */
    fun openCustomTab(url: String, mode: String) {

        val fadeIn: Int
        when(mode) {
            "geolocation" -> {
                fadeIn = R.anim.fade_in_ct_location
            }
            "geolocation_admin" -> {
                fadeIn = R.anim.fade_in_ct_location_admin
            }
            "camera" -> {
                fadeIn = R.anim.fade_in_ct_camera
            }
            "camera_admin" -> {
                fadeIn = R.anim.fade_in_ct_camera_admin
            }
            else -> {
                Log.e("CustomTabHelper", "Invalid mode: $mode")
                return
            }
        }

        val customTabsIntent = CustomTabsIntent.Builder(customTabsSession)
            .setStartAnimations(context, fadeIn, R.anim.fade_out)
            .setShowTitle(true)
            .build()

        // Launch URL in Custom Tab
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }

    /**
     * Unbinds the Custom Tabs service connection.
     */
    fun unbindService() {
        context.unbindService(customTabsServiceConnection)
    }
}
