import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get/get.dart';
import 'package:wenia_assignment/core/api/UsageTimelinePage.dart';
import 'package:wenia_assignment/core/api/hourly_app_usage_screen.dart';
import 'package:wenia_assignment/core/api/usage_service_widget.dart';
import 'package:wenia_assignment/presentation/home/list_apps_controller.dart';

class NativeCommunicationScreen extends StatefulWidget {
  @override
  _NativeCommunicationScreenState createState() =>
      _NativeCommunicationScreenState();
}

class _NativeCommunicationScreenState extends State<NativeCommunicationScreen> {
  static const platform = MethodChannel('com.alecodeando/native');
  static const platformTime = MethodChannel('com.example.timeService');
  String _messageFromKotlin = "Esperando mensaje desde Kotlin...";

  final ListAppsController listAppscontroller = Get.find();
  @override
  void initState() {
    super.initState();
    listenForKotlinMessages(); // Empezamos a escuchar mensajes de Kotlin
  }

  // Método para enviar mensaje a Kotlin
  Future<void> sendMessageToKotlin() async {
    try {
      String response =
          await platform.invokeMethod('sendToKotlin', "Hola desde Flutter");
      print("Respuesta desde Kotlin: $response");
    } catch (e) {
      print("Error al enviar mensaje a Kotlin: ${e.toString()}");
    }
  }

  // Método para enviar mensaje a Kotlin
  Future<void> sendMessageToKotlinStartTime() async {
    try {
      String response =
          await platformTime.invokeMethod('startService', "Hola desde Flutter");
      print("Respuesta desde Kotlin: $response");
    } catch (e) {
      print("Error al enviar mensaje a Kotlin: ${e.toString()}");
    }
  }

  // Método para escuchar mensajes desde Kotlin
  Future<void> listenForKotlinMessages() async {
    platform.setMethodCallHandler((call) async {
      if (call.method == 'receiveFromKotlin') {
        String message = call.arguments;
        setState(() {
          _messageFromKotlin = message;
        });
        print('Mensaje recibido de Kotlin: $message');
      }
    });
  }

  static const platformFloating =
      MethodChannel('com.example.wenia_assignment/floating_widget');

  Future<void> showFloatingWidget() async {
    try {
      await platformFloating.invokeMethod('showFloatingWidget');
    } on PlatformException catch (e) {
      print("Error: '${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Flutter - Kotlin Communication'),
      ),
      floatingActionButton: FloatingActionButton(onPressed: () {
        sendMessageToKotlinStartTime();
      }),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            GestureDetector(
              onTap: () {
                startOverlay();
              },
              child: Container(
                padding: EdgeInsets.all(5),
                color: Colors.blue,
                child: Text('SHOW'),
              ),
            ),
            UsagePageSelectable(listAppscontroller.mapListAppsUsageByHour),
            // Expanded(child: HourlyUsagePage())
          ],
        ),
      ),
    );
  }

  static const _channel = MethodChannel('app/overlay');
  Future<void> startOverlay() async {
    await _channel.invokeMethod('startOverlayService');
  }
}
