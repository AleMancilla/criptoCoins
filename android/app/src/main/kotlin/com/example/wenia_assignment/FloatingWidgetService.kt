// package com.alecodeando.weniatest
// import android.app.Service
// import android.content.Context
// import android.content.Intent
// import android.content.SharedPreferences
// import android.os.Build
// import android.os.Handler
// import android.os.IBinder
// import android.util.Log
// import android.view.LayoutInflater
// import android.view.View
// import android.view.WindowManager
// import android.widget.TextView
// import android.graphics.PixelFormat
// import android.view.Gravity
// import android.view.MotionEvent
// import android.graphics.Color
// import android.graphics.drawable.GradientDrawable

// class FloatingWidgetService : Service() {

//     private lateinit var floatingView: View
//     private lateinit var windowManager: WindowManager
//     private val handler = Handler()
//     private lateinit var sharedPreferences: SharedPreferences

//     private var initialX = 0
//     private var initialY = 0
//     private var initialTouchX = 0f
//     private var initialTouchY = 0f

//     override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//         try {
//             intent?.let {
//                 when (it.action) {
//                     "CLOSE_WIDGET" -> closeWidget()
//                     else -> {
//                         val usageTime = it.getStringExtra("usage_time")
//                         usageTime?.let { time ->
//                             val timeParts = time.split(":").map { part -> part.toIntOrNull() ?: 0 }
//                             val usageSeconds = timeParts[0] * 3600 + timeParts[1] * 60 + timeParts[2]
//                             updateUsageTime(time, usageSeconds)
//                         }
//                     }
//                 }
//             }
//         } catch (e: Exception) {
//             Log.e("FloatingWidgetService", "Error en onStartCommand: ${e.message}")
//             e.printStackTrace()
//         }
//         return START_STICKY
//     }


//     override fun onCreate() {
//         super.onCreate()

//         // Inicializar SharedPreferences
//         sharedPreferences = getSharedPreferences("FloatingWidgetPrefs", Context.MODE_PRIVATE)

//         // Inflar el diseño del widget flotante
//         floatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget_layout, null)

//         // Configurar parámetros de la ventana
//         val params = WindowManager.LayoutParams(
//             WindowManager.LayoutParams.WRAP_CONTENT,
//             WindowManager.LayoutParams.WRAP_CONTENT,
//             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
//             WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//             PixelFormat.TRANSLUCENT
//         )

//         // Restaurar la última posición guardada
//         val savedX = sharedPreferences.getInt("widget_position_x", 0)
//         val savedY = sharedPreferences.getInt("widget_position_y", 100)

//         params.gravity = Gravity.TOP or Gravity.START
//         params.x = savedX
//         params.y = savedY

//         // Agregar la vista al WindowManager
//         windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
//         windowManager.addView(floatingView, params)

//         // Hacer que el widget flotante sea arrastrable
//         floatingView.setOnTouchListener { _, event ->
//             when (event.action) {
//                 MotionEvent.ACTION_DOWN -> {
//                     initialX = params.x
//                     initialY = params.y
//                     initialTouchX = event.rawX
//                     initialTouchY = event.rawY
//                     true
//                 }
//                 MotionEvent.ACTION_MOVE -> {
//                     params.x = initialX + (event.rawX - initialTouchX).toInt()
//                     params.y = initialY + (event.rawY - initialTouchY).toInt()
//                     windowManager.updateViewLayout(floatingView, params)

//                     // Guardar la posición actual en SharedPreferences
//                     saveWidgetPosition(params.x, params.y)
//                     true
//                 }
//                 else -> false
//             }
//         }
//     }

//     private fun saveWidgetPosition(x: Int, y: Int) {
//         with(sharedPreferences.edit()) {
//             putInt("widget_position_x", x)
//             putInt("widget_position_y", y)
//             apply() // Guardar los cambios de forma asíncrona
//         }
//     }

//     fun updateUsageTime(usageTime: String, usageSeconds: Int) {
//         val usageTextView = floatingView.findViewById<TextView>(R.id.usage_time_text_view)
//         usageTextView.text = usageTime

//         val backgroundColor = when {
//             usageSeconds < 1800 -> Color.parseColor("#1B5E20")
//             usageSeconds in 1800..2700 -> Color.parseColor("#E6C701")
//             else -> Color.parseColor("#E60F00")
//         }
//         floatingView.setBackgroundColor(backgroundColor)
//     }

//     private fun closeWidget() {
//         Log.d("FloatingWidgetService", "Cerrando widget flotante")
//         if (::floatingView.isInitialized) {
//             windowManager.removeView(floatingView)
//         }
//         stopSelf()
//     }

//     override fun onDestroy() {
//         super.onDestroy()
//         if (::floatingView.isInitialized) {
//             try {
//                 windowManager.removeView(floatingView)
//             } catch (e: Exception) {
//                 Log.e("FloatingWidgetService", "Error al eliminar la vista: ${e.message}")
//             }
//         }
//     }

//     override fun onBind(intent: Intent?): IBinder? {
//         return null
//     }
// }
