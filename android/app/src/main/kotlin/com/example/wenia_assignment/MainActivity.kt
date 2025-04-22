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

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.alecodeando/native"
    private val CHANNELTIMESERVICE = "com.example.timeService"
    private val CHANNELFLOATING = "com.example.wenia_assignment/floating_widget"

    private val CHANNELDB = "com.example.wenia_assignment/database"
    
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
                    Log.e("Error in channel", "ENTROOOOOOOOOOOOOOOOOOO XD")

                    val appMonitorServiceIntent = Intent(this, AppMonitorService::class.java)
                    startService(appMonitorServiceIntent)
                    result.success("Usage limits updated in service.")
                }
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


    }

    override fun onResume() {
        super.onResume()
        startSendingMessagesToFlutter() // Iniciar envío de mensajes después de reanudar la actividad
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
                
                "startService" -> {
                    openAccessibilitySettings(this)
                    result.success("1") // 1 = true, 0 = false
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

         
        // Establecer el canal de método para comunicarse con Flutter
        // MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNELDB).setMethodCallHandler { call, result ->
        //     when (call.method) {
        //        "getUsers" -> {
        //             val users = getUsers() // Asegúrate de que este método exista
        //             result.success(users)
        //         }
        //        "updateUsageLimits" -> {
        //             Log.e("Error in channel", "ENTROOOOOOOOOOOOOOOOOOO XD")

        //             val appMonitorServiceIntent = Intent(this, AppMonitorService::class.java)
        //             startService(appMonitorServiceIntent)
        //             result.success("Usage limits updated in service.")
        //         }
        //         else -> {
        //             result.notImplemented()
        //         }
        //     }
        // }
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

}
