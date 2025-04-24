import 'package:app_usage/app_usage.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get/route_manager.dart';
import 'package:overlay_pop_up/overlay_pop_up.dart';
import 'package:wenia_assignment/core/database/databaseservice.dart';
import 'package:wenia_assignment/core/database/opendatabase.dart';
import 'package:wenia_assignment/core/utils/user_preferens.dart';
import 'package:wenia_assignment/presentation/splash/splash_screen.dart';
import 'package:firebase_core/firebase_core.dart';
import 'firebase_options.dart';

import 'dart:async';

// Ale1qa@gmail.com
void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final db = await initOpenDatabase();
  await DatabaseService.initDatabase(); // Inicializa la base de datos en Kotlin

  // await DatabaseService.insertUser('JohnDoe', 'john.doe@example.com');
  // await DatabaseService.insertAllowedApp('com.whatsapp', 'WhatsApp', null);
  // await DatabaseService.insertUsageLimit(1, 1, 120,
  //     10); // Ejemplo: usuario ID 1, app ID 1, límite diario 120 minutos, intervalo de notificación 10 minutos

  // Ahora `db` está listo para operaciones de lectura/escritura.
  final prefs = UserPreferences();
  await prefs.initPreferences();

  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      title: 'Social Stop',
      home: SplashScreen(),
      // theme: Themes().ligthMode,
    );
  }
}

///
/// the name is required to be `overlayPopUp` and has `@pragma("vm:entry-point")`
///
@pragma("vm:entry-point")
void overlayPopUp() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(MaterialApp(
    debugShowCheckedModeBanner: false,
    home: OverlayWidget(),
  ));
}

class OverlayWidget extends StatefulWidget {
  @override
  _OverlayWidgetState createState() => _OverlayWidgetState();
}

class _OverlayWidgetState extends State<OverlayWidget> {
  String _currentApp = "Unknown";
  Timer? _timer;
  AppUsage appUsage = AppUsage();

  @override
  void initState() {
    super.initState();
    _checkPermissionsAndStartTracking();
    _startPeriodicUpdate();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  // Verificar y solicitar permisos
  Future<void> _checkPermissionsAndStartTracking() async {
    _startAppUsageTracking();
  }

  //
  // Temporizador para actualizar periódicamente la app en primer plano
  void _startPeriodicUpdate() {
    _timer = Timer.periodic(Duration(seconds: 5), (Timer t) {
      _startAppUsageTracking();
    });
  }

  // Obtener el nombre de la app en primer plano
  void _startAppUsageTracking() async {
    try {
      DateTime endDate = DateTime.now();
      DateTime startTime = DateTime(endDate.year, endDate.month,
          endDate.day); // Inicia desde las 00:00 de hoy

      List<AppUsageInfo> infoList =
          await appUsage.getAppUsage(startTime, endDate);
      setState(() {
        _currentApp = infoList.last.appName;
        print(' ====== infoList.last.appName = ${infoList.last.appName}');
      });
    } on AppUsageException catch (e) {
      print('Error: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: Container(
        padding: const EdgeInsets.all(15),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              'App en primer plano: $_currentApp',
              style: TextStyle(fontSize: 14),
              textAlign: TextAlign.center,
            ),
            SizedBox(height: 10),
            FloatingActionButton(
              shape: CircleBorder(),
              backgroundColor: Colors.red[900],
              elevation: 12,
              onPressed: () async => await OverlayPopUp.closeOverlay(),
              child: Text('X',
                  style: TextStyle(color: Colors.white, fontSize: 20)),
            ),
          ],
        ),
      ),
    );
  }
}
