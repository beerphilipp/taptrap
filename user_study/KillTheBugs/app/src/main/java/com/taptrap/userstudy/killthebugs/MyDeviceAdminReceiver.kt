package com.taptrap.userstudy.killthebugs

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device Admin Receiver for handling device administration events.
 * Used for Level 3.
 */
class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    /**
     * Called when the device admin is enabled.
     */
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        val levelActivity = Intent(context, LevelActivity::class.java)
        levelActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        levelActivity.putExtra("points", 1)
        levelActivity.putExtra("level", 4)
        context.startActivity(levelActivity)
    }

    /**
     * Called when the device admin is disabled.
     */
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}