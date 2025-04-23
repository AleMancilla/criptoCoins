package com.alecodeando.weniatest

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.LayoutInflater
import android.view.View
import android.view.Gravity
import android.graphics.PixelFormat
import android.os.Build
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat
import com.alecodeando.weniatest.R


class OverlayService : Service() {
  private lateinit var windowManager: WindowManager
  private lateinit var overlayView: View

  override fun onCreate() {
    super.onCreate()
    windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    overlayView = LayoutInflater.from(this)
      .inflate(R.layout.view_overlay_counter, null)

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      else
        WindowManager.LayoutParams.TYPE_PHONE,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT
    ).apply {
      gravity = Gravity.TOP or Gravity.START
      x = 0
      y = 100
    }

    windowManager.addView(overlayView, params)
    startForeground(1, buildNotification()) // Necesario para no ser matado
  }

  override fun onDestroy() {
    super.onDestroy()
    windowManager.removeView(overlayView)
  }

  override fun onBind(intent: Intent?) = null

  private fun buildNotification(): Notification {
    val chanId = "overlay_service"
    val chanName = "Overlay Service"
    val cm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      cm.createNotificationChannel(
        NotificationChannel(chanId, chanName, NotificationManager.IMPORTANCE_NONE)
      )
    }
    return Notification.Builder(this, chanId)
      .setContentTitle("Overlay activo")
      .setSmallIcon(R.mipmap.ic_launcher)
      .build()
  }
}
