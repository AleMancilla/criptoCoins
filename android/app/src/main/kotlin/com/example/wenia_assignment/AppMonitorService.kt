package com.alecodeando.weniatest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.*

class AppMonitorService : Service() {

    companion object {
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "app_monitor_channel"
    }

    // --- LÓGICA DE MONITOREO ---
    private val handler = Handler(Looper.getMainLooper())
    private var monitorRunnable: Runnable? = null

    private lateinit var usageStatsManager: UsageStatsManager
    private var lastAppPackage: String? = null
    private var appTimerRunnable: Runnable? = null
    private var totalUsageTime = 0
    private var isScreenOn = true
    private var hasShownLimitPopup = false
    private var usageLimits: MutableMap<String, Int> = mutableMapOf()
    private var extraTimePerApp: MutableMap<String, Int> = mutableMapOf()
    private var allowedPackages: List<String> = emptyList()

    // Receiver para eventos de pantalla
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    stopTracking(lastAppPackage)
                    Log.d("AppMonitorService", "Pantalla apagada")
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    lastAppPackage?.let { startTracking(it) }
                    Log.d("AppMonitorService", "Pantalla encendida")
                }
            }
        }
    }

    // Propiedades para overlay
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var overlayParams: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        loadAllowedPackagesFromDatabase()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupOverlayParams()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1) Recarga límites de uso siempre
        loadAllowedPackagesFromDatabase()
        Log.d("AppMonitorService", "Allowed after reload: $allowedPackages")

        // 2) Construye y lanza tu notificación de foreground
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("App Monitor Activo")
            .setContentText("Supervisando uso de aplicaciones…")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)

        // 3) Inicia monitoreo
        startMonitoring()
        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID,
                "Monitor de uso de apps",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Notificaciones del servicio de monitoreo" }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }
    }

    private fun startMonitoring() {
        monitorRunnable = object : Runnable {
            override fun run() {
                if (isScreenOn) checkForegroundApp()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(monitorRunnable!!)
    }

    private fun checkForegroundApp() {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - 1000, now)
        var lastEvent: UsageEvents.Event? = null
        while (events.hasNextEvent()) {
            val e = UsageEvents.Event()
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) lastEvent = e
        }
        lastEvent?.let { ev ->
            val pkg = ev.packageName
            if (pkg == packageName) return
            if (allowedPackages.contains(pkg)) {
                if (pkg != lastAppPackage) {
                    stopTracking(lastAppPackage)
                    startTracking(pkg)
                    lastAppPackage = pkg
                }
            } else {
                Log.d("AppMonitorServiceDENEGADA", "App DENEGADA: $pkg  list $allowedPackages ")
                loadAllowedPackagesFromDatabase()
                if (!allowedPackages.contains(pkg)) {
                    stopTracking(lastAppPackage)
                    lastAppPackage = null
                }
            }
        }
    }

    private fun startTracking(pkg: String) {
        totalUsageTime = getAppUsageTime(pkg)
        showOverlay()

        val limitSecs = usageLimits[pkg] ?: (15 * 60)
        appTimerRunnable = object : Runnable {
            override fun run() {
                totalUsageTime++
                val txt = formatTime(totalUsageTime)
                Log.d("AppMonitorService", "App en uso: $pkg - Tiempo: $txt")
                updateOverlay(txt)

                val extra = extraTimePerApp[pkg] ?: 0
                if (totalUsageTime >= limitSecs && extra <= 0 && !hasShownLimitPopup) {
                    showUsageLimitPopup(pkg)
                    hasShownLimitPopup = true
                } else if (extra > 0) {
                    extraTimePerApp[pkg] = extra - 1
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(appTimerRunnable!!)
    }

    private fun stopTracking(pkg: String?) {
        appTimerRunnable?.let { handler.removeCallbacks(it) }
        hideOverlay()
        pkg?.let {
            Log.d("AppMonitorService", "Cerrando $it → total ${formatTime(getAppUsageTime(it))}")
        }
    }

    private fun getAppUsageTime(pkg: String): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            cal.timeInMillis,
            System.currentTimeMillis()
        )
        return stats.firstOrNull { it.packageName == pkg }
            ?.let { (it.totalTimeInForeground / 1000).toInt() } ?: 0
    }

    private fun formatTime(sec: Int): String {
        val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    // Métodos de overlay
    private fun setupOverlayParams() {
        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return
        overlayView = LayoutInflater.from(this)
            .inflate(R.layout.view_overlay_counter, null)
        windowManager.addView(overlayView, overlayParams)
    }

    private fun updateOverlay(time: String) {
        overlayView?.findViewById<TextView>(R.id.overlay_text)?.text = time
    }

    private fun hideOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    
    // Función para cargar los nombres de paquetes desde la base de datos
    private fun loadAllowedPackagesFromDatabase() {
        val dbHelper = DatabaseHelper(this)
        val usageLimitsFromDb = dbHelper.getAllUsageLimits() // Obtener todos los UsageLimit

        allowedPackages = usageLimitsFromDb.map { it.packageName } // Asignar los nombres de los paquetes

        // Llenar el mapa de límites de uso
        usageLimitsFromDb.forEach {
            usageLimits[it.packageName] = it.limitTime // Asigna el límite a su paquete correspondiente
        }

        Log.d("___________________AppMonitorService", "Allowed packages loaded: $allowedPackages")
    }
    override fun onDestroy() {
        super.onDestroy()
        monitorRunnable?.let { handler.removeCallbacks(it) }
        appTimerRunnable?.let { handler.removeCallbacks(it) }
        unregisterReceiver(screenStateReceiver)
        hideOverlay()
    }


    // Función para mostrar el popup cuando el tiempo de uso es excedido
    private fun showUsageLimitPopup(packageName: String) {
        // Crear un LayoutInflater para inflar el diseño personalizado del popup
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.usage_limit_popup, null)

        // Configurar los parámetros del popup para que se muestre sobre cualquier app
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        // Agregar la vista al WindowManager
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(popupView, params)

        // Configurar el botón de cerrar dentro del popup
        val closeButton = popupView.findViewById<Button>(R.id.close_popup_button)
        closeButton.setOnClickListener {
            windowManager.removeView(popupView)
        }

        // Configurar el botón de "1 minuto más"
        val oneMoreMinuteButton = popupView.findViewById<Button>(R.id.one_more_minute_button)
        oneMoreMinuteButton.setOnClickListener {
            extendUsageTime(packageName)
            windowManager.removeView(popupView)
        }

        // Vibrar cuando el popup se muestre
        vibratePhone()
    }


     // Método para extender el tiempo de uso en 1 minuto
    private fun extendUsageTime(packageName: String) {
        val currentExtraTime = extraTimePerApp[packageName] ?: 0
        extraTimePerApp[packageName] = currentExtraTime + 60 // Añadir 60 segundos al tiempo extra de la app actual
        hasShownLimitPopup = false // Permitir que el popup se muestre nuevamente solo si se agota el tiempo extra
        Log.d("AppMonitorService", "Se ha añadido 1 minuto extra a $packageName")
    }

    private fun vibratePhone() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Para versiones de Android O (API 26) y superiores
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 200, 500, 200, 1500) // Vibrar 500ms, pausar 200ms, repetir
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255) // 255 es la vibración más fuerte

            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1)) // -1 para no repetir
    } else {
            // Para versiones anteriores
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 200, 500, 200, 1500) // Vibrar 500ms, pausar 200ms, repetir
            vibrator.vibrate(pattern, -1) // -1 indica que no se repite el patrón
        }
    }
}



data class UsageLimit(
    val limitId: Int,
    val packageName: String,
    val limitTime: Int
)
