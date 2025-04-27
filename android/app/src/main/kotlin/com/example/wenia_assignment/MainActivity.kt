package com.alecodeando.weniatest

import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.net.Uri

import android.app.Service
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import io.flutter.embedding.engine.FlutterEngine

import android.graphics.PixelFormat
import android.view.Gravity

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager

import android.content.ComponentName
import android.text.TextUtils
import androidx.core.content.ContextCompat


import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import java.util.*


import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Base64

import android.content.pm.ApplicationInfo

import java.util.Calendar
import java.lang.reflect.Modifier


import android.app.AppOpsManager
import android.os.Process



typealias HourlyData = Map<String, Any>

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.alecodeando/native"
    private val CHANNELTIMESERVICE = "com.example.timeService"
    private val CHANNELFLOATING = "com.example.wenia_assignment/floating_widget"

    private val CHANNELDB = "com.example.wenia_assignment/database"

    private val CHANNELOVERLAY = "app/overlay"

    private val USAGE_CHANNEL = "mi.paquete/usage"

    private val CHANNEL_USAGE_HOUR = "mi.paquete/usage_hourly"


  private val CHANNEL_PERMISSION = "com.example.app/usage_access"

    
    // Definir la vista flotante
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Establecer el canal de método para comunicarse con Flutter
        MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNELDB).setMethodCallHandler { call, result ->
            when (call.method) {
                "initDatabase" -> {
                    dbHelper.writableDatabase // Abre la base de datos
                    result.success("Base de datos inicializada en Kotlin")
                }
                "updateUsageLimits" -> {
                        // Ya no abrimos ajustes de accesibilidad, 
                        // simplemente arrancamos AppMonitorService con overlay
                        startAppMonitorWithOverlay { started ->
                            if (started) result.success("Service arrancado")
                            else       result.error("PERM_DENIED", "Overlay permission required", null)
                        }
                    }
                // "updateUsageLimits" -> {
                //     Log.e("Error in channel", "ENTROOOOOOOOOOOOOOOOOOO XD")

                //     val appMonitorServiceIntent = Intent(this, AppMonitorService::class.java)
                //     startService(appMonitorServiceIntent)
                //     result.success("Usage limits updated in service.")
                // }
                "insertUser" -> {
                    val username = call.argument<String>("username") ?: ""
                    val email = call.argument<String>("email") ?: ""
                    val newRowId = dbHelper.insertUser(username, email)
                    if (newRowId != -1L) {
                        result.success("Usuario insertado con ID: $newRowId")
                    } else {
                        result.error("INSERT_ERROR", "Error al insertar el usuario", null)
                    }
                }
                "insertAllowedApp" -> {
                    val packageName = call.argument<String>("packageName") ?: ""
                    val appName = call.argument<String>("appName") ?: ""
                    val userId = call.argument<Int?>("userId")
                    val newRowId = dbHelper.insertAllowedApp(packageName, appName, userId)
                    if (newRowId != -1L) {
                        result.success("Aplicación permitida insertada con ID: $newRowId")
                    } else {
                        result.error("INSERT_ERROR", "Error al insertar la aplicación permitida", null)
                    }
                }
                "deleteAllowedApp" -> {
                    val packageName = call.argument<String>("packageName") ?: ""
                    val rowsDeleted = dbHelper.deleteAllowedApp(packageName)
                    if (rowsDeleted > 0) {
                        result.success("Aplicación permitida eliminada con éxito.")
                    } else {
                        result.error("DELETE_ERROR", "Error al eliminar la aplicación permitida o no encontrada.", null)
                    }
                }
                "deleteUsageLimits" -> {
                    val packageName = call.argument<String>("packageName") ?: ""
                    val rowsDeleted = dbHelper.deleteUsageLimits(packageName)
                    if (rowsDeleted > 0) {
                        result.success("deleteUsageLimits permitida eliminada con éxito.")
                    } else {
                        result.error("DELETE_ERROR", "Error al eliminar la aplicación permitida o no encontrada.", null)
                    }
                }
                "insertUsageLimit" -> {
                    val packageName = call.argument<String>("packageName") ?: ""
                    val userId = call.argument<Int>("userId") ?: return@setMethodCallHandler result.error("INVALID_ARGS", "userId es requerido", null)
                    val appId = call.argument<Int>("appId") ?: return@setMethodCallHandler result.error("INVALID_ARGS", "appId es requerido", null)
                    val dailyLimit = call.argument<Int>("dailyLimit") ?: return@setMethodCallHandler result.error("INVALID_ARGS", "dailyLimit es requerido", null)
                    val notificationInterval = call.argument<Int>("notificationInterval") ?: return@setMethodCallHandler result.error("INVALID_ARGS", "notificationInterval es requerido", null)

                    val newRowId = dbHelper.insertUsageLimit(packageName,userId, appId, dailyLimit, notificationInterval)
                    if (newRowId != -1L) {
                        result.success("Límite de uso insertado con ID: $newRowId")
                    } else {
                        result.error("INSERT_ERROR", "Error al insertar el límite de uso", null)
                    }
                }
                "getUsers" -> {
                    val users = dbHelper.getUsers() // Implementa este método en DatabaseHelper
                    result.success(users)
                }
                "getAllowedApps" -> {
                    Log.e("==>> AllowedApps3", "cursor")
                    val response = dbHelper.getAllowedApps() // Implementa este método en DatabaseHelper
                    result.success(response)
                }
                "getUsageLimits" -> {
                    Log.e("==>> UsageLimits3", "cursor")
                    val response = dbHelper.getUsageLimits() // Implementa este método en DatabaseHelper
                    result.success(response)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        Intent(this, UsageControlService::class.java).also { intent ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        }


    }

    override fun onResume() {
        super.onResume()
        startSendingMessagesToFlutter() // Iniciar envío de mensajes después de reanudar la actividad
    }


    private fun startAppMonitorWithOverlay(onResult: (Boolean) -> Unit) {
    if (!Settings.canDrawOverlays(this)) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        startActivity(intent)
        onResult(false)
    } else {
        val svcIntent = Intent(this, AppMonitorService::class.java)
        ContextCompat.startForegroundService(this, svcIntent)
        onResult(true)
    }
}

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        dbHelper = DatabaseHelper(this)


        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "sendToKotlin") {
                val message = call.arguments as String
                println("Mensaje recibido de Flutter: $message")

                // Enviar respuesta de vuelta a Flutter
                result.success("Mensaje recibido correctamente en Kotlin")
            } else {
                result.notImplemented()
            }
        }

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNELTIMESERVICE).setMethodCallHandler { call, result ->
            when (call.method) {
                
                // "startService" -> {
                //     openAccessibilitySettings(this)
                //     result.success("1") // 1 = true, 0 = false
                // }
                "startService" -> {
                        startAppMonitorWithOverlay { started ->
                            result.success(if (started) 1 else 0)
                        }
                    }
                "isAccessibilityEnabled" -> {
                    // Cambia por el nombre de tu servicio real
                    val serviceId = "com.alecodeando.weniatest/.AppMonitorService"
                    val isEnabled = isAccessibilityServiceEnabled(this)
                    result.success(if (isEnabled) 1 else 0)
                }
                "openAccessibilitySettings" -> {
                    openAccessibilitySettings(this)
                    result.success("1")
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNELFLOATING).setMethodCallHandler { call, result ->
            when (call.method) {
                "showFloatingWidget" -> {
                    showFloatingWidget() // Llama al método que inicia el widget flotante
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNELOVERLAY).setMethodCallHandler { call, result ->
            when (call.method) {
                // "startOverlayService" -> {
                //     val intent = Intent(this, OverlayService::class.java)
                //     ContextCompat.startForegroundService(this, intent)
                //     result.success(null)
                // }
                // "stopOverlayService" -> {
                //     stopService(Intent(this, OverlayService::class.java))
                //     result.success(null)
                // }
                "startOverlayService" -> {
                    startAppMonitorWithOverlay { started ->
                        if (started) result.success(null)
                        else         result.error("PERM_DENIED", "Overlay permission required", null)
                    }
                }
                "stopOverlayService" -> {
                    stopService(Intent(this, AppMonitorService::class.java))
                    result.success(null)
                }
                "updateCounter" -> {
                    val newCount = call.argument<Int>("value") ?: 0
                    // Envía un broadcast o usa bindService para pasar el valor al overlayView
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, USAGE_CHANNEL)
        .setMethodCallHandler { call, result ->
            when (call.method) {
            "getUsageToday" -> {
                try {
                // Llamamos a tu función (podrías pasarle args si quisieras)
                val mapLong: Map<String, Long> = getUsageToday()
                // Flutter admite Map<String, Long> directamente
                result.success(mapLong)
                } catch (e: Exception) {
                result.error("USAGE_ERROR", e.message, null)
                }
            }
            else -> result.notImplemented()
            }
        }

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_USAGE_HOUR)
        .setMethodCallHandler { call, result ->
            if (call.method == "getHourlyUsage") {
                try {
                    val data = getHourlyUsageWithMeta()
                    result.success(data)
                } catch (e: Exception) {
                    result.error("HOURLY_USAGE_ERROR", e.message, null)
                }
            } 
            
            if (call.method == "getHourlyForegroundUsage") {
                try {
                        val usageList = applicationContext.getHourlyForegroundUsage()
                        // Devuelve una lista de mapas JSON-serializables
                        result.success(usageList)
                    } catch (e: Exception) {
                        result.error("USAGE_ERROR", e.message, null)
                    }
            } 
            
            else {
                result.notImplemented()
            }
        }

        MethodChannel(
        flutterEngine.dartExecutor.binaryMessenger,
        CHANNEL_PERMISSION
        ).setMethodCallHandler { call, result ->
        when (call.method) {
          "openUsageAccessSettings" -> {
            // Abre ajustes de Usage Access
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
            result.success(null)
          }
          "isUsageAccessGranted" -> {
            // Comprueba si realmente está concedido
            result.success(isUsageStatsPermissionGranted(this))
          }
          else -> result.notImplemented()
        }
        }

        
    }
    
    private fun isUsageStatsPermissionGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), context.packageName
        )
        } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), context.packageName
        )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getUsers(): List<Map<String, Any>> {
        return dbHelper.getUsers() // Asegúrate de que este método esté bien implementado en DatabaseHelper
    }

    // Método para enviar datos desde Kotlin a Flutter
    fun sendToFlutter(message: String) {
        try {
        MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNEL)
            .invokeMethod("receiveFromKotlin", message)
        }
        catch (e: Exception) {
            Log.e("Error in channel", "Error")
        }
    }

    // Llamar a sendToFlutter automáticamente
    fun startSendingMessagesToFlutter() {
        Handler(Looper.getMainLooper()).postDelayed({
            sendToFlutter("Mensaje enviado desde Kotlin después de 5 segundos")
        }, 5000) // Enviar mensaje después de 5 segundos
    }

    fun startBackgroundTimeService(context: Context) {
        val intent = Intent(context, TimeService::class.java)
        context.startService(intent)
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }



    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val serviceId = "com.alecodeando.weniatest/.AppMonitorService"
        val expectedComponentName = ComponentName.unflattenFromString(serviceId)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        for (service in colonSplitter) {
            if (ComponentName.unflattenFromString(service) == expectedComponentName) {
                return true
            }
        }
        return false
    }
    
    private fun showFloatingWidget() {
        createFloatingView() // Inicializa la vista flotante
        startCountDown(10) // Comienza el conteo de 10 segundos
    }

    private fun createFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget_layout, null)

        // Configura parámetros de diseño para el widget flotante
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = 0 // Coordenada x inicial
        params.y = 100 // Coordenada y inicial

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        // Asume que tienes un TextView en tu layout llamado "timeTextView"
        // val timeTextView: TextView = floatingView.findViewById(R.id.timeTextView)
        // timeTextView.text = "0 segundos" // Inicializar el texto
    }

    // private fun updateUsageTime(time: String) {
    //     val timeTextView: TextView = floatingView.findViewById(R.id.timeTextView)
    //     timeTextView.text = time
    // }
    fun updateUsageTime(usageTime: String) {
        val usageTextView = floatingView.findViewById<TextView>(R.id.usage_time_text_view) // Cambia esto si el ID es diferente
        usageTextView.text = usageTime // Actualiza el texto del TextView con el tiempo de uso
    }

   private fun startCountDown(seconds: Int) {
    var countdown = seconds
    val handler = Handler(Looper.getMainLooper())

    val runnable = object : Runnable {
        override fun run() {
            if (countdown >= 0) {
                updateUsageTime("$countdown segundos")
                countdown--
                handler.postDelayed(this, 1000) // Decrementa el contador cada segundo
            } else {
                // Detiene el conteo y elimina el widget flotante
                handler.removeCallbacks(this)
                removeFloatingWidget() // Llama al método para eliminar el widget flotante
            }
        }
    }
    handler.post(runnable)
}

private fun removeFloatingWidget() {
    // Elimina el widget flotante de la ventana
    val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    windowManager.removeView(floatingView)
}


  // Tu función nativa que ya calcula el uso “hoy”
  private fun getUsageToday(): Map<String, Long> {
    // 1) Calcula inicio de hoy
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = cal.timeInMillis

    // 2) Obtén el UsageStatsManager
    val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // 3) Pide el agregado de stats en [start, now]
    val aggregated: Map<String, UsageStats> =
        usm.queryAndAggregateUsageStats(start, now)

    // 4) Devuelve sólo el totalTimeInForeground por paquete
    return aggregated.mapValues { entry ->
        entry.value.totalTimeInForeground
    }
    }


  private fun getHourlyUsageWithMeta(): List<Map<String, Any>> {
    // 1) cálculo de inicio de hoy
    val now   = System.currentTimeMillis()
    val cal   = Calendar.getInstance().apply {
      timeInMillis = now
      set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val start = cal.timeInMillis

    // 2) recogida de eventos
    val usm    = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val events = usm.queryEvents(start, now)
    val ev     = UsageEvents.Event()

    data class Span(val pkg: String, val t0: Long, val t1: Long)
    val lastFg   = mutableMapOf<String, Long>()
    val spans    = mutableListOf<Span>()
    val launches = mutableListOf<Pair<String, Long>>()

    while (events.hasNextEvent()) {
      events.getNextEvent(ev)
      val pkg = ev.packageName ?: continue
      when (ev.eventType) {
        UsageEvents.Event.MOVE_TO_FOREGROUND -> {
          lastFg[pkg] = ev.timeStamp
          launches += pkg to ev.timeStamp
        }
        UsageEvents.Event.MOVE_TO_BACKGROUND -> {
          val enter = lastFg.remove(pkg) ?: start
          if (ev.timeStamp > enter) spans += Span(pkg, enter, ev.timeStamp)
        }
      }
    }
    lastFg.forEach { (pkg, enter) ->
      spans += Span(pkg, enter, now)
    }

    // 3) acumula por paquete y hora
    val raw = mutableMapOf<String, MutableMap<Int, MutableList<Long>>>()
    fun slot(pkg: String, hour: Int) {
      raw.getOrPut(pkg) { mutableMapOf() }
         .getOrPut(hour) { mutableListOf(0L, 0L) }
    }

    for ((pkg, t0, t1) in spans) {
      var s = maxOf(t0, start)
      val e = minOf(t1, now)
      while (s < e) {
        val c = Calendar.getInstance().apply { timeInMillis = s }
        val h = c.get(Calendar.HOUR_OF_DAY)
        c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
        val endSlot = minOf(c.timeInMillis, e)
        slot(pkg, h)
        raw[pkg]!![h]!![0] += (endSlot - s)
        s = endSlot + 1
      }
    }
    // lanzamientos
    for ((pkg, ts) in launches) {
      if (ts in start..now) {
        val h = Calendar.getInstance().apply { timeInMillis = ts }
                .get(Calendar.HOUR_OF_DAY)
        slot(pkg, h)
        val lst = raw[pkg]!![h]!!           // MutableList<Long>
        lst[1] = lst[1] + 1 
      }
    }

    // 4) empaqueta con nombre e icono
    val pm = packageManager
    val out = mutableListOf<Map<String, Any>>()
    for ((pkg, hours) in raw) {
    // 1) Obtengo ApplicationInfo (si falla, lo dejo pasar, pero puedes ajustarlo)
    val ai = try { pm.getApplicationInfo(pkg, 0) } catch (_: Exception) { null }

    // 2) Si es app de sistema, la ignoro
    if (ai != null) {
        val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSys = (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        if (isSystem || isUpdatedSys) continue
    }

    // 3) Sigo empaquetando nombre, icono y stats
    val label = ai?.let { pm.getApplicationLabel(it).toString() } ?: pkg

      // icono a base64
      val iconB64 = ai?.let {
        val d = pm.getApplicationIcon(it)
        val bmp = when(d) {
          is BitmapDrawable -> d.bitmap
          else -> {
            val b = Bitmap.createBitmap(
              d.intrinsicWidth.coerceAtLeast(1),
              d.intrinsicHeight.coerceAtLeast(1),
              Bitmap.Config.ARGB_8888
            )
            val c2 = Canvas(b)
            d.setBounds(0,0,c2.width,c2.height)
            d.draw(c2)
            b
          }
        }
        ByteArrayOutputStream().use { st ->
          bmp.compress(Bitmap.CompressFormat.PNG, 100, st)
          Base64.encodeToString(st.toByteArray(), Base64.NO_WRAP)
        }
      } ?: ""

      for ((hour, vals) in hours) {
        out += mapOf(
          "packageName" to pkg,
          "appName"     to label,
          "icon"        to iconB64,
          "hour"        to hour,
          "usage"       to vals[0],
          "launches"    to vals[1].toInt()
        )
      }
    }
    return out
  }

fun Context.getHourlyForegroundUsage(): List<HourlyData> {
  // 1) Definir rango del día
  val now = System.currentTimeMillis()
  val cal = Calendar.getInstance().apply {
    timeInMillis = now
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
  val start = cal.timeInMillis

  val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

  // 2) Leer todos los eventos (package, tipo, timestamp, class)
  data class Evt(val pkg: String?, val type: Int, val ts: Long, val cls: String?)
  val events = mutableListOf<Evt>()
  val iter = usm.queryEvents(start, now)
  val ev = UsageEvents.Event()
  while (iter.hasNextEvent()) {
    iter.getNextEvent(ev)
    events.add(Evt(ev.packageName, ev.eventType, ev.timeStamp, ev.className))
  }

  // 3) Calcular spans de pantalla activa
  data class Span(val pkg: String?, val t0: Long, val t1: Long)
  val screenSpans = mutableListOf<Span>()
  var lastScreenOn = start
  var screenOn = true
  events.forEach { e ->
    when (e.type) {
      UsageEvents.Event.SCREEN_NON_INTERACTIVE -> if (screenOn) {
        screenSpans.add(Span(null, lastScreenOn, e.ts))
        screenOn = false
      }
      UsageEvents.Event.SCREEN_INTERACTIVE     -> if (!screenOn) {
        lastScreenOn = e.ts
        screenOn = true
      }
    }
  }
  if (screenOn) screenSpans.add(Span(null, lastScreenOn, now))

  // 4) Calcular spans de app en foreground
  val appSpans = mutableListOf<Span>()
  val lastFg  = mutableMapOf<String, Long>()
  events.forEach { e ->
    val pkg = e.pkg ?: return@forEach
    when (e.type) {
      UsageEvents.Event.MOVE_TO_FOREGROUND -> lastFg[pkg] = e.ts
      UsageEvents.Event.MOVE_TO_BACKGROUND -> {
        val t0 = lastFg.remove(pkg) ?: start
        if (e.ts > t0) appSpans.add(Span(pkg, t0, e.ts))
      }
    }
  }
  lastFg.forEach { (pkg, t0) -> appSpans.add(Span(pkg, t0, now)) }

  // 5) Intersectar spans de pantalla vs app
  fun intersect(a: Span, s: Span): Span? {
    val s0 = maxOf(a.t0, s.t0)
    val s1 = minOf(a.t1, s.t1)
    return if (s1 > s0) Span(a.pkg, s0, s1) else null
  }
  val goodSpans = mutableListOf<Span>()
  appSpans.forEach { asp ->
    screenSpans.forEach { ssp ->
      intersect(asp, ssp)?.let { goodSpans.add(it) }
    }
  }

  // 6) Sumar uso y contar spans por hora
  val usageMap = mutableMapOf<String, MutableMap<Int, Long>>()
  val spanCounts = mutableMapOf<String, MutableMap<Int, Int>>()
  goodSpans.forEach { span ->
    val pkg = span.pkg ?: return@forEach
    var s = span.t0
    while (s < span.t1) {
      val c = Calendar.getInstance().apply { timeInMillis = s }
      val hr = c.get(Calendar.HOUR_OF_DAY)
      c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
      val slotEnd = minOf(c.timeInMillis, span.t1)
      usageMap.getOrPut(pkg) { mutableMapOf() }
        .merge(hr, slotEnd - s) { old, extra -> old + extra }
      spanCounts.getOrPut(pkg) { mutableMapOf() }
        .merge(hr, 1) { old, inc -> old + inc }
      s = slotEnd + 1
    }
  }

  // 7) Empaquetar resultados, filtrando launchDetails por la Main Activity
  val pm = packageManager
  val out = mutableListOf<HourlyData>()
  usageMap.forEach { (pkg, byHour) ->
    // Ignorar apps de sistema
    val ai = try { pm.getApplicationInfo(pkg, 0) } catch (_: Exception) { null }
    if (ai != null && (ai.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) return@forEach
    val label = ai?.let { pm.getApplicationLabel(it).toString() } ?: pkg

    // Icono en Base64
    val iconB64 = ai?.let {
      val d   = pm.getApplicationIcon(it)
      val bmp = (d as? BitmapDrawable)?.bitmap ?: Bitmap.createBitmap(
        d.intrinsicWidth.coerceAtLeast(1), d.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888
      ).also { b -> Canvas(b).apply { d.setBounds(0,0,width,height); d.draw(this) } }
      ByteArrayOutputStream().use { st ->
        bmp.compress(Bitmap.CompressFormat.PNG,100,st)
        Base64.encodeToString(st.toByteArray(), Base64.NO_WRAP)
      }
    } ?: ""

    // Totales del día
    val totalUsage    = byHour.values.sum()
    val totalLaunches = events.count { it.pkg == pkg && (it.type == UsageEvents.Event.MOVE_TO_FOREGROUND || it.type == UsageEvents.Event.MOVE_TO_BACKGROUND)
      && it.cls == pm.getLaunchIntentForPackage(pkg)?.component?.className }
    val firstUse      = goodSpans.filter { it.pkg == pkg }.minOfOrNull { it.t0 } ?: start
    val lastUse       = goodSpans.filter { it.pkg == pkg }.maxOfOrNull { it.t1 } ?: start

    // Preparar detalles filtrados por Main Activity
    val launchClass = pm.getLaunchIntentForPackage(pkg)?.component?.className
    val launchDetails = events.filter { it.pkg == pkg
        && (it.type == UsageEvents.Event.MOVE_TO_FOREGROUND || it.type == UsageEvents.Event.MOVE_TO_BACKGROUND)
        && it.cls == launchClass
      }
      .map { ev -> mutableMapOf<String, Any>(
          "eventType" to ev.type,
          "eventTypeName" to getEventTypeName(ev.type),
          "timestamp" to ev.ts,
          "className" to (ev.cls ?: "")
      ) }

    // Empaquetar por hora
    byHour.forEach { (hour, ms) ->
  // Calcula cuántos 'resumes' (launches) caen en esta hora:
  val launchesPerHour = launchDetails.count { detail ->
    detail["eventType"] == UsageEvents.Event.MOVE_TO_FOREGROUND &&
    Calendar.getInstance().apply {
      timeInMillis = detail["timestamp"] as Long
    }.get(Calendar.HOUR_OF_DAY) == hour
  }

    val hourMap = mutableMapOf<String, Any>()
    hourMap["packageName"] = pkg
    hourMap["appName"]     = label
    hourMap["icon"]        = iconB64
    hourMap["hour"]        = hour
    hourMap["usage"]       = ms
    hourMap["spanCount"]   = spanCounts[pkg]?.get(hour) ?: 0
    hourMap["totalUsage"]  = totalUsage
    hourMap["launches"]    = launchesPerHour
    hourMap["totalLaunches"] = totalLaunches
    hourMap["firstUse"]    = firstUse
    hourMap["lastUse"]     = lastUse
    hourMap["launchDetails"] = launchDetails

    out.add(hourMap)
    }
  }
  return out
}
fun getEventTypeName(type: Int): String {
  return UsageEvents.Event::class.java.fields
    .asSequence()
    .filter { f -> Modifier.isStatic(f.modifiers) && f.type == Int::class.javaPrimitiveType }
    .firstOrNull { f ->
      try { f.getInt(null) == type } catch (_: Exception) { false }
    }
    ?.name
    ?: "UNKNOWN($type)"
}

}

data class Evt(
  val pkg: String?,
  val type: Int,
  val ts: Long,
  val cls: String?  
)

