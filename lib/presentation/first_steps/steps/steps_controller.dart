import 'package:app_usage/app_usage.dart';
import 'package:easy_permission_validator/easy_permission_validator.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
// import 'package:flutter_accessibility_service/flutter_accessibility_service.dart';
import 'package:get/get.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:system_alert_window/system_alert_window.dart';

class StepsController extends GetxController {
  RxBool permisionUsageIsComplete = false.obs;
  RxBool permisionSuperPosicionComplete = false.obs;
  RxBool permisionAccesibility = false.obs;

  // @override
  // void onInit() async {
  //   super.onInit();
  // }

  askForSuperpositionPermision() async {
    bool? status = await SystemAlertWindow.checkPermissions();
    print(' ==== status == $status');
    if (!(status ?? false)) {
      SystemAlertWindow.requestPermissions();
    }
  }

  permisionUseApp(BuildContext context) async {
    final permissionValidator = EasyPermissionValidator(
      context: context,
      appName: 'Social Stop',
      appNameColor: Colors.red,
      cancelText: 'Cancelar',
      enableLocationMessage:
          'Debe habilitar los permisos necesarios para utilizar la App.',
      goToSettingsText: 'Ir a Configuraciones',
      permissionSettingsMessage:
          'Necesita habilitar los permisos necesarios para que la aplicación funcione correctamente',
    );
    var result = await permissionValidator.systemAlertWindow();
    if (result) {
      // Do something;
      permisionSuperPosicionComplete.value = result;
      print(result);
    }
  }

  permisionAccesibilidad(BuildContext context) async {
    print(' ---- entro aqui');
    final bool status = await checkAccessibility();
    print(' ---- respuesta aqui $status');
    if (!status) {
      final bool request = await checkAccessibility();
      permisionAccesibility.value = request;
    } else {
      permisionAccesibility.value = true;
    }
  }

  static const platform = MethodChannel('com.example.timeService');

  static Future<bool> checkAccessibility() async {
    try {
      // final bool isAccessibilityEnabled =
      await platform.invokeMethod('startService');
      return true;
    } on PlatformException catch (e) {
      print("Error al verificar la accesibilidad: ${e.message}");
      return false;
    }
  }

  Future<void> askForPermision() async {
    // permisionUsageIsComplete.value = await verifyPermisions();
    verifyPermisions().then(
      (value) {
        permisionUsageIsComplete.value = value;
      },
    );
    print(' - -------- ${permisionUsageIsComplete.value}');
  }

  Future<bool> verifyPermisions() async {
    List<AppUsageInfo> _list = await getUsageStats();
    if (_list.isNotEmpty) {
      return true;
    }
    return false;
  }

  Future<List<AppUsageInfo>> getUsageStats() async {
    try {
      DateTime endDate = DateTime.now();
      DateTime startTime = DateTime(endDate.year, endDate.month,
          endDate.day); // Inicia desde las 00:00 de hoy

      List<AppUsageInfo> infoList =
          await AppUsage().getAppUsage(startTime, endDate);

      // setState(() => _infos = infoList);

      for (var info in infoList) {
        print(info.toString());
      }
      return (infoList);
    } on AppUsageException catch (exception) {
      print(' ====== > $exception');
      // throw exception;
      return [];
    }
  }
}
