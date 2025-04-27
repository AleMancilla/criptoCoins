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
import android.view.MotionEvent
import android.graphics.Color
import android.content.SharedPreferences

import android.widget.FrameLayout

import android.provider.Settings
import androidx.core.app.NotificationCompat
import android.net.Uri

import android.app.PendingIntent



class AppMonitorService : Service() {

    companion object {
        private const val NOTIF_ID = 1
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "app_monitor_channel"
        // 🔧 Nombre de SharedPreferences y prefijo de clave
        private const val PREFS_NAME = "app_monitor_prefs"
        private const val KEY_PREFIX_USAGE = "usage_"
        private const val PERM_NOTIFICATION_ID = 2
        private const val KEY_PREFIX_BLOCKED = "blocked_"
    }

    // 🔧 SharedPreferences para persistencia
    private lateinit var prefs: SharedPreferences

    // --- LÓGICA DE MONITOREO ---
    private val handler = Handler(Looper.getMainLooper())
    private var monitorRunnable: Runnable? = null

    private lateinit var usageStatsManager: UsageStatsManager
    private var lastAppPackage: String? = null
    private var appTimerRunnable: Runnable? = null
    private var totalUsageTime = 0
    private var isScreenOn = true
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
    private var hasShownLimitPopup = false
    private var hasShownHalfLimitPopup = false
    

    private val halfLimitMessages = listOf(
        "Desconecta para conectar: tu vida está más allá de la pantalla.",
        "Cada minuto fuera de redes es una oportunidad para vivir algo real.",
        "Las mejores conversaciones suceden cara a cara, no en likes.",
        "Tu tiempo es valioso: úsalo en quien realmente te importa.",
        "¿Sabes cuánto has vivido hoy? ¡Deja el scroll y descúbrelo!",
        "Las redes son herramientas, no tu vida. Toma el control.",
        "Mira a tu alrededor: el mundo espera fuera de tu teléfono.",
        "Menos seguidores, más amigos. Menos pantalla, más experiencias.",
        "La felicidad no se mide en notificaciones, sino en momentos.",
        "Desbloquea tu creatividad: las ideas llegan cuando desconectas.",
        "Tu presencia física vale más que cualquier mensaje en línea.",
        "Hoy es un buen día para llamar a alguien en vez de escribir.",
        "Las redes sociales son un viaje, pero la vida es el destino.",
        "Recuerda: nadie en su lecho de muerte desearía haber scrolleado más.",
        "SocialStop te ayuda a vivir, no a sobrevivir en likes."
    )

    
    // Receiver para eventos de pantalla...
    // Propiedades para overlay...
    
    override fun onCreate() {
        super.onCreate()
        // 🔧 Inicializa SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

        // Si no hay permiso de overlay, mostrar notificación para pedirlo
        if (!Settings.canDrawOverlays(this)) {
            showPermissionNotification()
            stopSelf()
            return
        }
        // Arrancar como servicio foreground con notificación
        startForeground(NOTIFICATION_ID, buildMonitoringNotification())
        
    }
    // override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    //     // Si el sistema mata el servicio, lo reinicie
    //     return START_STICKY
    // }

    override fun onBind(intent: Intent): IBinder? = null

    private fun buildMonitoringNotification(): Notification {
        // Crea el canal y la notificación que informa que el monitor está activo
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "App Monitor",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitor de uso activo")
            .setContentText("La superposición está funcionando en segundo plano")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun showPermissionNotification() {
        // Notificación que lleva al usuario a conceder overlay permission
        val permIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(
            this, 0, permIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Permiso de superposición requerido")
            .setContentText("Toca para habilitar el permiso de overlay")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        startForeground(PERM_NOTIFICATION_ID, notif)
    }

    // 🔧 Aseguramos guardar al “swipear” la app
    override fun onTaskRemoved(rootIntent: Intent) {
        // Cuando el usuario remueve la tarea, relanzar el servicio
        val restartIntent = Intent(applicationContext, AppMonitorService::class.java).apply {
            setPackage(packageName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }

        lastAppPackage?.let { saveUsageTime(it, totalUsageTime) }

        super.onTaskRemoved(rootIntent)
        stopSelf()
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
    // 🔧 Restaura de prefs o, si no existe, usa UsageStats
        totalUsageTime = prefs.getInt("$KEY_PREFIX_USAGE$pkg", -1).let {
            if (it >= 0) it else getAppUsageTime(pkg)
        }
        // ① Si ya está bloqueada, mostramos el popup de bloqueo y salimos
        if (prefs.getBoolean("$KEY_PREFIX_BLOCKED$pkg", false)) {
            showBlockedPopup(pkg)
            return
        }


        hasShownHalfLimitPopup = false

        showOverlay()

        val limitSecs = usageLimits[pkg] ?: (15 * 60)
        val halfLimitSecs = limitSecs / 2

        appTimerRunnable = object : Runnable {
            override fun run() {
                totalUsageTime++
                val txt = formatTime(totalUsageTime)
                updateOverlay(txt, totalUsageTime, limitSecs)

                // 🔧 Cada tick guardamos el nuevo valor
                saveUsageTime(pkg, totalUsageTime)

                val extra = extraTimePerApp[pkg] ?: 0
                Log.d("AppMonitorService", "App en uso: $pkg - Tiempo: $txt ___ límite $limitSecs, halfLímite $halfLimitSecs ___ extras $extra ___ mostrados: límitePopup=$hasShownLimitPopup, halfPopup=$hasShownHalfLimitPopup")

                // 1) Si pasamos la mitad del límite, mostramos el primer aviso
                // if (totalUsageTime > halfLimitSecs && totalUsageTime < limitSecs && !hasShownHalfLimitPopup) {
                if (totalUsageTime > halfLimitSecs && totalUsageTime < limitSecs && !hasShownHalfLimitPopup) {
                    showHalfLimitPopup(pkg)
                    hasShownHalfLimitPopup = true
                }
                // 2) Si llegamos al límite total, mostramos el aviso de límite
                else if (totalUsageTime >= limitSecs && extra <= 0 && !hasShownLimitPopup) {
                    showUsageLimitPopup(pkg)
                    hasShownLimitPopup = true
                }
                // 3) Si hay tiempo extra, lo descontamos
                else if (extra > 0) {
                    extraTimePerApp[pkg] = extra - 1
                }

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(appTimerRunnable!!)
        lastAppPackage = pkg
    }

    private fun showHalfLimitPopup(packageName: String) {
    // Escoge mensaje aleatorio
    val message = halfLimitMessages.random()


    // Infla tu nuevo layout de popup (full-screen)
    val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val popupView = inflater.inflate(R.layout.usage_half_limit_popup, null)

    // Asigna el mensaje aleatorio al TextView
    val tv = popupView.findViewById<TextView>(R.id.half_message)
    tv.text = message

    // Parámetros full-screen
    val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.CENTER }

    val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    windowManager.addView(popupView, params)

    // Botón “Continuar”
    popupView.findViewById<Button>(R.id.continue_button).setOnClickListener {
        windowManager.removeView(popupView)
        // (Opcional) relanzar la app:
        packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let(::startActivity)
    }

    // Vibrar al mostrar (si lo deseas)
    vibratePhone()
}


    private fun stopTracking(pkg: String?) {
        appTimerRunnable?.let { handler.removeCallbacks(it) }
        pkg?.let {
            // 🔧 Guardamos al detener el tracking
            saveUsageTime(it, totalUsageTime)
            Log.d("AppMonitorService", "Cerrando $it → total ${formatTime(totalUsageTime)}")
        }
        hideOverlay()
    }

    // 🔧 Funciones auxiliares de persistencia
    private fun saveUsageTime(pkg: String, time: Int) {
        prefs.edit()
            .putInt("$KEY_PREFIX_USAGE$pkg", time)
            .apply()
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

        // Infla la vista
        overlayView = LayoutInflater.from(this)
            .inflate(R.layout.view_overlay_counter, null)
        windowManager.addView(overlayView, overlayParams)

        // Métricas de pantalla
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Variables de estado para el drag
        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f

        overlayView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Guarda posición inicial
                    initialX = overlayParams.x
                    initialY = overlayParams.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Calcula nueva posición sin límites
                    val newX = initialX + (event.rawX - touchStartX).toInt()
                    val newY = initialY + (event.rawY - touchStartY).toInt()

                    // Calcula tamaño de la vista (ya inflada y medida)
                    val viewWidth = v.width
                    val viewHeight = v.height

                    // Clamp: recorta X e Y dentro de la pantalla
                    overlayParams.x = newX.coerceIn(0, screenWidth - viewWidth)
                    overlayParams.y = newY.coerceIn(0, screenHeight - viewHeight)

                    windowManager.updateViewLayout(v, overlayParams)
                    true
                }
                else -> false
            }
        }
    }



    private fun updateOverlay(timeStr: String, seconds: Int, limitSeconds: Int) {
        // 1) Actualiza el texto
        val tv = overlayView
            ?.findViewById<TextView>(R.id.overlay_text)
            ?: return
        tv.text = timeStr
        // siempre texto blanco
        tv.setTextColor(Color.WHITE)

        // 2) Calcula color de fondo según thresholds
        val orangeThreshold = limitSeconds/2    // 15 minutos
        val redThreshold    = limitSeconds    // 30 minutos (ajusta a 30 si son segundos)

        val bgColor = when {
            seconds >= redThreshold    -> Color.parseColor("#E60F00")
            seconds >= orangeThreshold -> Color.parseColor("#E6C701")
            else                       -> Color.parseColor("#1B5E20")
        }

        // 3) Aplica el color de fondo al contenedor
        val root = overlayView
            ?.findViewById<FrameLayout>(R.id.overlay_root)
            ?: overlayView!!
        root.setBackgroundColor(bgColor)
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
        // 🔧 Guardado final por si onTaskRemoved no se llamó
        lastAppPackage?.let { saveUsageTime(it, totalUsageTime) }
        monitorRunnable?.let { handler.removeCallbacks(it) }
        appTimerRunnable?.let { handler.removeCallbacks(it) }
        unregisterReceiver(screenStateReceiver)
        hideOverlay()
        super.onDestroy()
    }


    private fun showUsageLimitPopup(packageName: String) {
        // 1) Envía al Home para pausar la app de fondo
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)

        // 2) Infla tu layout de popup
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.usage_limit_popup, null)

        // 3) Parámetros full-screen y focusable
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        // 4) Muestra el popup
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(popupView, params)

        // Botón “1 Minuto Más”
        val oneMoreMinuteButton = popupView.findViewById<Button>(R.id.one_more_minute_button)
        oneMoreMinuteButton.setOnClickListener {
            // ✅ Amplía el tiempo
            extendUsageTime(packageName)
            // ✅ Quita el popup
            windowManager.removeView(popupView)

            // 🔧 Relanza automáticamente la app
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            }
        }

        // Botón Cerrar
        val closeButton = popupView.findViewById<Button>(R.id.close_popup_button)
        closeButton.setOnClickListener {
            prefs.edit()
                .putBoolean("$KEY_PREFIX_BLOCKED$packageName", true)
                .apply()

            windowManager.removeView(popupView)
        }

        // Vibrar al mostrar
        vibratePhone()
    }

    private fun showBlockedPopup(packageName: String) {
        // Lleva al home para pausar la app de fondo
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })

        // Obtén el nombre legible de la app en mayúsculas
        val appName = try {
            packageManager
                .getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
                .toString()
                .uppercase()
        } catch (e: Exception) {
            packageName.uppercase()
        }

        // Infla el layout bloqueado
        val popupView = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.usage_blocked_popup, null)

        // Asigna el texto dinámico
        popupView.findViewById<TextView>(R.id.blocked_message).text =
            "APP '$appName' BLOQUEADA"

        // Parám. full-screen
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.addView(popupView, params)

        // Botón “Cerrar” que quita el overlay y regresa al home
        popupView.findViewById<Button>(R.id.close_blocked_button)
            .setOnClickListener {
                wm.removeView(popupView)
                // Opcional: matar proceso
                android.os.Process.killProcess(android.os.Process.myPid())
            }

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
