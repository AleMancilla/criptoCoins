import 'package:app_usage/app_usage.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:installed_apps/index.dart';

class HomeController extends GetxController {
  RxList<AppUsageInfo> listAppUsageInfo = <AppUsageInfo>[].obs;
  @override
  void onInit() async {
    super.onInit();
    getUsageStats();
  }

  Future<void> getUsageStats() async {
    try {
      print('--------1');

      DateTime endDate = DateTime.now();
      DateTime startTime = DateTime(endDate.year, endDate.month,
          endDate.day); // Inicia desde las 00:00 de hoy
      print('--------3');
      List<AppUsageInfo> infoList =
          await AppUsage().getAppUsage(startTime, endDate);
      print('--------4');

      // setState(() => _infos = infoList);

      for (var info in infoList) {
        print(info.toString());
      }
      print('--------5');
      listAppUsageInfo.value = infoList;
      print(infoList);
    } on AppUsageException catch (exception) {
      print(' ====== > $exception');
      // throw exception;
    }
  }

  // Función para obtener el ícono de la aplicación
  Future<Widget> _getAppIcon(String packageName) async {
    AppInfo? app = await InstalledApps.getAppInfo(packageName, null);
    if (app?.icon != null) {
      return Image.memory(app!.icon!, width: 40, height: 40);
    } else {
      return Icon(Icons.apps);
    }
  }
}
