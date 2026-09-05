package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity

/**
 * Automatically launches MasjidKU TV when the Android TV device / STB turns on,
 * reboots after a power outage, or wakes up from quick boot.
 *
 * Strategy:
 *  1. goAsync() to avoid ANR on main thread during boot.
 *  2. Sleep 1s for system services to settle.
 *  3. Try startActivity directly (works on Android TV / leanback).
 *  4. On Android 10+ strict BAL devices, fallback to BootLauncherService
 *     which starts as a ForegroundService and re-launches MainActivity.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.ACTION_POWER_CONNECTED"
        )
        if (action !in validActions) return

        val pendingResult = goAsync()
        Thread {
            try {
                // Give system services 1 second to settle after boot
                Thread.sleep(1000)

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                }

                try {
                    context.startActivity(launchIntent)
                    Log.d("BootReceiver", "MainActivity launched on boot ($action)")
                } catch (e: Exception) {
                    // Fallback: Android 10+ may block background activity launch
                    Log.w("BootReceiver", "Direct launch blocked, trying service fallback: ${e.message}")
                    try {
                        val serviceIntent = Intent(context, BootLauncherService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } catch (ex: Exception) {
                        Log.e("BootReceiver", "Service fallback also failed: ${ex.message}")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
