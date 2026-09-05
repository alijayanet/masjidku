package com.example.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.MainActivity

/**
 * Fallback service used by BootReceiver when direct activity launch is blocked
 * by Android 10+ Background Activity Launch (BAL) restrictions.
 *
 * Starts as a ForegroundService, relaunches MainActivity, then stops itself.
 */
class BootLauncherService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithNotification()
        try {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(launchIntent)
            Log.d("BootLauncherService", "MainActivity launched via ForegroundService fallback")
        } catch (e: Exception) {
            Log.e("BootLauncherService", "Failed to launch MainActivity: ${e.message}")
        } finally {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val channelId = "masjidku_boot_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MasjidKU TV Autostart",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Digunakan untuk menjalankan aplikasi otomatis setelah TV dinyalakan" }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("MasjidKU TV")
                .setContentText("Memulai aplikasi otomatis…")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("MasjidKU TV")
                .setContentText("Memulai aplikasi otomatis…")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        }
        startForeground(999, notification)
    }
}
