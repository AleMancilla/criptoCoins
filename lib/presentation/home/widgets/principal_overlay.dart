import 'package:flutter/material.dart';
import 'package:flutter_time_picker_spinner/flutter_time_picker_spinner.dart';
import 'package:get/get.dart';
import 'package:installed_apps/index.dart';
import 'package:wenia_assignment/core/database/api_database.dart';
import 'package:wenia_assignment/core/database/databaseservice.dart';
import 'package:wenia_assignment/presentation/home/list_apps_controller.dart';
import 'overlay_controller.dart';

class PrincipalOverlay extends StatefulWidget {
  PrincipalOverlay({Key? key}) : super(key: key);

  @override
  State<PrincipalOverlay> createState() => _PrincipalOverlayState();
}

class _PrincipalOverlayState extends State<PrincipalOverlay> {
  final OverlayController controller = Get.put(OverlayController());
  final ListAppsController listAppscontroller = Get.find();
  bool edit = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          Expanded(
            child: Obx(
              () {
                // Filtrar solo aplicaciones en appsSelectable
                final selectedApps = listAppscontroller.apps
                    .where((app) => listAppscontroller.appsSelectable
                        .contains(app.packageName))
                    .toList();

                return ListView.builder(
                  itemCount: selectedApps.length,
                  itemBuilder: (context, index) {
                    final app = selectedApps[index];
                    final packageName = app.packageName;
                    final appName = app.name;
                    final maxTime =
                        listAppscontroller.maxUsageTime[packageName] ??
                            Duration();

                    return GestureDetector(
                      onTap: edit
                          ? () async {
                              // Editar el tiempo máximo de uso con un selector de tiempo
                              await _showTimePicker(
                                  context, packageName, maxTime, app.name);
                              setState(() {});
                            }
                          : null,
                      child: Container(
                        margin: EdgeInsets.only(bottom: 5),
                        decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(10),
                            boxShadow: [
                              BoxShadow(
                                blurRadius: 2,
                                color: Colors.black12,
                              )
                            ]),
                        padding:
                            EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                        child: Row(
                          children: [
                            Container(
                              width: 60,
                              height: 60,
                              child: Stack(
                                alignment: AlignmentDirectional.center,
                                children: [
                                  Icon(
                                    Icons.timer_outlined,
                                    size: 50,
                                    color: Colors.grey,
                                  ),
                                  Positioned(
                                    bottom: 0,
                                    right: 0,
                                    child: (app.icon != null)
                                        ? Image.memory(
                                            app.icon!,
                                            width: 20,
                                            height: 20,
                                          )
                                        : const Icon(Icons
                                            .android), // Icono genérico si no hay ícono
                                  )
                                ],
                              ),
                            ),
                            SizedBox(width: 10),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    appName,
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  Text(
                                    'Uso permitido: ${listAppscontroller.formatDuration(maxTime)}',
                                    style: TextStyle(fontSize: 12),
                                  ),
                                ],
                              ),
                            ),
                            SizedBox(width: 10),
                            if (edit) Icon(Icons.edit),
                          ],
                        ),
                      ),
                    );
                  },
                );
              },
            ),
          ),
          !edit
              ? GestureDetector(
                  onTap: () {
                    edit = true;
                    setState(() {});
                  },
                  child: Container(
                    margin: EdgeInsets.only(bottom: 10),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(10),
                      color: Colors.blueGrey[900],
                    ),
                    padding: EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                    child: Text(
                      'Editar ✎',
                      style: TextStyle(color: Colors.white),
                    ),
                  ),
                )
              : GestureDetector(
                  onTap: () async {
                    edit = false;
                    setState(() {});

                    final ListAppsController listAppscontroller = Get.find();
                    List<AppInfo> selectedApps = listAppscontroller.apps
                        .where((app) => listAppscontroller.appsSelectable
                            .contains(app.packageName))
                        .toList();

                    await ApiDatabase.insertUsageLimitList(
                        selectedApps, listAppscontroller.maxUsageTime);

                    DatabaseService.updateUsageLimits();
                  },
                  child: Container(
                    margin: EdgeInsets.only(bottom: 10),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(10),
                      color: Colors.green[900],
                    ),
                    padding: EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                    child: Text(
                      'Guardar ✎',
                      style: TextStyle(color: Colors.white),
                    ),
                  ),
                )
        ],
      ),
    );
  }

  // Mostrar un TimePickerDialog para seleccionar el tiempo máximo
  Future<void> _showTimePicker(BuildContext context, String packageName,
      Duration initialTime, String nameApp) async {
    int hours = initialTime.inHours;
    int minutes = initialTime.inMinutes.remainder(60);
    int seconds = initialTime.inSeconds.remainder(60);

    await showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text('$nameApp'),
          content: Container(
            height: 250, // Ajusta el tamaño según sea necesario
            child: Column(
              children: [
                Text(
                    'Tiempo anterior ${listAppscontroller.formatDuration(initialTime)}, seleccione un nuevo tiempo maximo de uso para esta app'),
                Expanded(
                  child: TimePickerSpinner(
                    time: DateTime(0, 1, 1, hours, minutes, seconds),
                    is24HourMode: true,
                    isForce2Digits: true,
                    isShowSeconds: true,
                    normalTextStyle:
                        TextStyle(fontSize: 20, color: Colors.black),
                    highlightedTextStyle:
                        TextStyle(fontSize: 24, color: Colors.blue),
                    spacing: 20,
                    onTimeChange: (time) {
                      hours = time.hour;
                      minutes = time.minute;
                      seconds = time.second;
                    },
                  ),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: Text('Cancelar'),
            ),
            TextButton(
              onPressed: () {
                Duration newMaxTime = Duration(
                  hours: hours,
                  minutes: minutes,
                  seconds: seconds,
                );
                listAppscontroller.updateMaxUsageTime(packageName, newMaxTime);
                Navigator.of(context).pop();
              },
              child: Text('Aceptar'),
            ),
          ],
        );
      },
    );
  }
}
