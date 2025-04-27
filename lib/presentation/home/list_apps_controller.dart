import 'dart:developer';

import 'package:get/get.dart';
import 'package:installed_apps/index.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:usage_stats/usage_stats.dart';
import 'package:wenia_assignment/core/api/hourly_app_usage.dart';
import 'package:wenia_assignment/core/api/usage_service.dart';
import 'package:wenia_assignment/core/database/api_database.dart';
import 'package:wenia_assignment/core/database/models/model_db_allowed_apps.dart';
import 'package:wenia_assignment/core/database/models/model_db_usage_limits.dart';

class ListAppsController extends GetxController {
  var apps = <AppInfo>[].obs;
  var filteredApps = <AppInfo>[].obs;
  var appsSelectable = <String>[
    'com.zhiliaoapp.musically',
    'com.whatsapp',
    'com.facebook.katana',
    'com.facebook.orca',
    'com.instagram.android',
  ].obs;
  var appUsageStats = <String, Duration>{}.obs;
  RxList<HourlyAppUsage> datePerHourList = <HourlyAppUsage>[].obs;

  RxMap<int, List<HourlyAppUsage>> mapListAppsUsageByHour =
      <int, List<HourlyAppUsage>>{}.obs;

  // Tiempo máximo de uso para cada app en appsSelectable
  var maxUsageTime = <String, Duration>{}.obs;

  @override
  void onInit() async {
    super.onInit();
    getInstalledApps();
    getUsageStats();
    getUsagePerHourByDay();
    await setDefaultAppSelectable();
    await setDefaultMaxUsageTime();
  }

  // Función para asignar un tiempo máximo por defecto para cada app en appsSelectable
  Future<void> setDefaultMaxUsageTime() async {
    List<ModelDbUsageLimits> _list = await ApiDatabase.getUsageLimits();
    print(_list);
    for (var app in appsSelectable) {
      maxUsageTime[app] = const Duration(minutes: 30);
    }
    if (_list.isNotEmpty) {
      _list.forEach(
        (element) {
          maxUsageTime[element.packageName!] =
              Duration(seconds: int.parse(element.dailyLimit!));
        },
      );
    }
  }

  Future<void> setDefaultAppSelectable() async {
    List<ModelDbAllowedApps> listModelDbAllowedApps =
        await ApiDatabase.getAllowedApps();
    if (listModelDbAllowedApps.isNotEmpty) {
      appsSelectable.value = listModelDbAllowedApps
          .map(
            (e) => e.packageName!,
          )
          .toList();
    }
  }

  // Función para actualizar el tiempo máximo de una aplicación específica
  void updateMaxUsageTime(String packageName, Duration maxTime) {
    if (appsSelectable.contains(packageName)) {
      maxUsageTime[packageName] = maxTime;
    }
  }

  void filterApps(String query) {
    if (query.isEmpty) {
      filteredApps.value = apps;
    } else {
      filteredApps.value = apps
          .where((app) => app.name.toLowerCase().contains(query.toLowerCase()))
          .toList();
    }
  }

  void selectApp(String package) {
    if (appsSelectable.contains(package)) {
      appsSelectable.remove(package);
      maxUsageTime.remove(package);
    } else {
      appsSelectable.add(package);
      maxUsageTime[package] = Duration(minutes: 30); // Tiempo predeterminado
    }
    sortAppsByUsage();
  }

  Future<void> getInstalledApps() async {
    List<AppInfo> installedApps = await InstalledApps.getInstalledApps(
      true,
      true,
    );

    apps.value = installedApps;
    filteredApps.value = installedApps;
    sortAppsByUsage();
  }

  Future<void> getUsagePermission() async {
    bool granted = await UsageStats.checkUsagePermission() ?? false;
    if (!granted) {
      await UsageStats.grantUsagePermission();
    }
  }

  Future<void> getUsagePerHourByDay() async {
    print('#####################################');
    List<HourlyAppUsage> _data = await UsageServiceHourly().fetchHourlyUsage();
    for (var u in _data) {
      if (u.launches == 0) {
        print('ignorado');
      } else {
        mapListAppsUsageByHour.putIfAbsent(u.hour, () => []).add(u);
      }
    }
    mapListAppsUsageByHour.refresh();
  }

  Future<void> getUsageStats() async {
    bool granted = await UsageStats.checkUsagePermission() ?? false;
    if (!granted) {
      await getUsagePermission();
    }

    DateTime endDate = DateTime.now();
    DateTime startDate = DateTime(endDate.year, endDate.month, endDate.day, 0,
        0, 0); // Inicia desde las 00:00 de hoy

    log('===   startDate = $startDate ====== =endDate == $endDate ==== DIFERENCE = ${startDate.difference(endDate)}');

    try {
      List<AppUsage> stats = await UsageService.fetchUsageToday();

      for (var stat in stats) {
        appUsageStats[stat.packageName] = stat.usage;
      }

      sortAppsByUsage();
      print(apps);
    } catch (e) {
      print("Error obteniendo estadísticas de uso: $e");
    }
  }

  void sortAppsByUsage() {
    apps.sort((a, b) {
      bool isASelectable = appsSelectable.contains(a.packageName);
      bool isBSelectable = appsSelectable.contains(b.packageName);

      if (isASelectable && !isBSelectable) {
        return -1;
      } else if (!isASelectable && isBSelectable) {
        return 1;
      }

      final durationA = appUsageStats[a.packageName] ?? Duration();
      final durationB = appUsageStats[b.packageName] ?? Duration();
      return durationB.compareTo(durationA);
    });
  }

  String formatDuration(Duration duration) {
    int hours = duration.inHours;
    int minutes = duration.inMinutes.remainder(60);

    if (hours > 0) {
      return '${hours} ${hours == 1 ? 'hora' : 'horas'} y ${minutes} ${minutes == 1 ? 'minuto' : 'minutos'}';
    } else {
      return '${minutes} ${minutes == 1 ? 'minuto' : 'minutos'}';
    }
  }
}
