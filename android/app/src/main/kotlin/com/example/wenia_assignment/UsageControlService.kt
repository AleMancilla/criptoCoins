package com.alecodeando.weniatest

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat

class UsageControlService : Service() {

  companion object {
    const val CHANNEL_ID = "uso_apps_channel"
    const val NOTIF_ID = 1001
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Control de Uso Activo")
      .setContentText("Monitoreando aplicaciones…")
      .setSmallIcon(R.mipmap.ic_launcher)
      .setOngoing(true)
      .build()

    startForeground(NOTIF_ID, notification)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // Aquí colocas tu lógica de monitoreo periódico...
    // Por ejemplo, un TimerTask, Coroutine, o Handler.postDelayed(...)
    return START_STICKY
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    // Si el usuario arrastra la app de recientes, reprogramamos el reinicio
    val restartIntent = Intent(applicationContext, UsageControlService::class.java)
    val pending = PendingIntent.getService(
      this, 0, restartIntent,
      PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
    )
    val mgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    mgr.set(
      AlarmManager.ELAPSED_REALTIME,
      SystemClock.elapsedRealtime() + 1000,
      pending
    )
    super.onTaskRemoved(rootIntent)
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val chan = NotificationChannel(
        CHANNEL_ID,
        "Control de Uso de Apps",
        NotificationManager.IMPORTANCE_LOW
      )
      chan.description = "Canal para la notificación persistente de monitoreo"
      val nm = getSystemService(NotificationManager::class.java)
      nm.createNotificationChannel(chan)
    }
  }
}
