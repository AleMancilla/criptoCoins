import 'dart:developer';

import 'package:device_apps/device_apps.dart';
import 'package:get/get.dart';
import 'package:wenia_assignment/core/database/databaseservice.dart';
import 'package:wenia_assignment/core/database/models/model_db_allowed_apps.dart';
import 'package:wenia_assignment/core/database/models/model_db_usage_limits.dart';

class ApiDatabase {
  static insertUser({required String name, required String email}) async {
    await DatabaseService.insertUser(name, email);
  }

  static getAllAppsInDB() async {
    List<ModelDbAllowedApps> listModelDbAllowedApps = await getAllowedApps();
    int i = 0;

    listModelDbAllowedApps.forEach(
      (element) {
        i += 1;
        log('$i App: ${element.appName} - ${element.packageName}');
      },
    );
  }

  static insertAllowedApp(
      {required String package, required String name}) async {
    // await DatabaseService.insertAllowedApp('com.whatsapp', 'WhatsApp', null);
    ModelDbAllowedApps? previous = await getSpecificAllowedApps(package);
    if (previous == null) {
      await DatabaseService.insertAllowedApp(package, name, null);
    }
  }

  static insertAllowedAppList(List<Application> selectedApps) async {
    // await DatabaseService.insertAllowedApp('com.whatsapp', 'WhatsApp', null);

    List<ModelDbAllowedApps> listModelDbAllowedApps = await getAllowedApps();

    listModelDbAllowedApps.forEach(
      (element) {
        Application? application = selectedApps.firstWhereOrNull(
          (app) => app.packageName == element.packageName,
        );
        if (application == null) {
          DatabaseService.deleteAllowedApp(element.packageName!);
        }
      },
    );
    selectedApps.forEach(
      (element) async {
        ModelDbAllowedApps? previous =
            await getSpecificAllowedApps(element.packageName);
        if (previous == null) {
          await DatabaseService.insertAllowedApp(
              element.packageName, element.appName, null);
        }
      },
    );
  }

  static Future insertUsageLimitList(
      List<Application> selectedApps, Map<String, Duration> maxUsageTime,
      {int? notificationInterval = 120}) async {
    // await DatabaseService.insertAllowedApp('com.whatsapp', 'WhatsApp', null);

    List<ModelDbUsageLimits> _list = await getUsageLimits();

    _list.forEach(
      (element) {
        Application? application = selectedApps.firstWhereOrNull(
          (app) => app.packageName == element.packageName,
        );
        if (application == null) {
          DatabaseService.deleteUsageLimits(element.packageName!);
        }
      },
    );
    print(selectedApps);
    selectedApps.forEach(
      (element) async {
        ModelDbUsageLimits? previous =
            await getSpecificUsageLimits(element.packageName);
        print('previous ============= > $previous');
        if (previous != null) {
          DatabaseService.deleteUsageLimits(element.packageName!);
        }
        // if (previous == null) {
        final app = element;
        final packageName = app.packageName;
        final appName = app.appName;
        Duration maxTime = maxUsageTime[packageName] ?? Duration(seconds: 120);

        ModelDbAllowedApps? data =
            await ApiDatabase.getSpecificAllowedApps(packageName);

        print(' ---- -usage INSERT insertUsageLimit');

        await DatabaseService.insertUsageLimit(
          1,
          data?.appId ?? 1,
          maxTime.inSeconds,
          notificationInterval ?? 120,
          data!.packageName!,
        );
        // }else{
        //   DatabaseService.deleteUsageLimits(element.packageName!);

        // }
      },
    );
  }

  static insertUsageLimit(
      {required String package,
      int dailyLimit = 1800,
      int notificationInterval = 120}) async {
    ModelDbAllowedApps? data =
        await ApiDatabase.getSpecificAllowedApps(package);

    await DatabaseService.insertUsageLimit(
        1, data?.appId ?? 1, dailyLimit, notificationInterval, package);
  }

  static Future<List<Map<String, dynamic>>> getUsers() async {
    return await DatabaseService.getUsers();
  }

  static Future<List<ModelDbAllowedApps>> getAllowedApps() async {
    // return await DatabaseService.getAllowedApps();
    List<Map<String, dynamic>> response =
        await DatabaseService.getAllowedApps();

    List<ModelDbAllowedApps> listModelDbAllowedApps = response
        .map(
          (e) => ModelDbAllowedApps.fromJson(e),
        )
        .toList();
    print(listModelDbAllowedApps);

    return listModelDbAllowedApps;
  }

  static Future<ModelDbAllowedApps?> getSpecificAllowedApps(
      String package) async {
    // return await DatabaseService.getAllowedApps();
    List<Map<String, dynamic>> response =
        await DatabaseService.getAllowedApps();

    List<ModelDbAllowedApps> listModelDbAllowedApps = response
        .map(
          (e) => ModelDbAllowedApps.fromJson(e),
        )
        .toList();

    ModelDbAllowedApps? responseModelAllowed =
        listModelDbAllowedApps.firstWhereOrNull(
      (element) => package == element.packageName,
    );

    return responseModelAllowed;
  }

  static Future<ModelDbUsageLimits?> getSpecificUsageLimits(
      String package) async {
    // return await DatabaseService.getAllowedApps();
    List<Map<String, dynamic>> response =
        await DatabaseService.getUsageLimits();

    List<ModelDbUsageLimits> listModelDbAllowedApps = response
        .map(
          (e) => ModelDbUsageLimits.fromJson(e),
        )
        .toList();

    ModelDbUsageLimits? responseUsageLimits =
        listModelDbAllowedApps.firstWhereOrNull(
      (element) => package == element.packageName,
    );

    return responseUsageLimits;
  }

  static Future<List<ModelDbUsageLimits>> getUsageLimits() async {
    List<Map<String, dynamic>> response =
        await DatabaseService.getUsageLimits();

    List<ModelDbUsageLimits> listUsageLimits = response
        .map(
          (e) => ModelDbUsageLimits.fromJson(e),
        )
        .toList();

    return listUsageLimits;
  }
}
