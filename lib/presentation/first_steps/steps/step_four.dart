import 'package:flutter/material.dart';
import 'package:flutter_time_picker_spinner/flutter_time_picker_spinner.dart';
import 'package:get/get.dart';
import 'package:wenia_assignment/presentation/first_steps/steps/steps_controller.dart';
import 'package:wenia_assignment/presentation/home/list_apps_controller.dart';

class StepFour extends StatefulWidget {
  StepFour({super.key});

  @override
  State<StepFour> createState() => _StepFourState();
}

class _StepFourState extends State<StepFour> {
  StepsController controller = Get.find();

  final ListAppsController listAppscontroller = Get.find();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 30),
      child: Column(
        children: [
          Expanded(
            child: Obx(
              () {
                // Filtrar solo aplicaciones en appsSelectable
                final selectedApps = listAppscontroller.apps
                    .where((app) => listAppscontroller.appsSelectable
                        .contains(app.packageName))
                    .toList();
                print(listAppscontroller.apps);

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
                      onTap: () async {
                        // Editar el tiempo máximo de uso con un selector de tiempo
                        await _showTimePicker(
                            context, packageName, maxTime, app.name);
                        setState(() {});
                      },
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
                                    'Uso permitido: ${formatDuration(maxTime)}',
                                    style: TextStyle(fontSize: 12),
                                  ),
                                ],
                              ),
                            ),
                            SizedBox(width: 10),
                            Icon(Icons.edit),
                          ],
                        ),
                      ),
                    );
                  },
                );
              },
            ),
          ),
          Column(
            children: [
              Text(
                'Tiempos de uso',
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 24,
                  fontFamily: 'paradice',
                ),
              ),
              SizedBox(height: 20),
              Text(
                'Seleccione las principales redes sociales que usa, el objetivo sera mantener un control de tiempo en cada una de las redes sociales',
                textAlign: TextAlign.center,
              ),
              // GestureDetector(
              //   onTap: () {},
              //   child: Container(
              //     padding: EdgeInsets.all(20),
              //     color: Colors.red,
              //   ),
              // )
            ],
          ),
        ],
      ),
    );
  }

  String formatDuration(Duration duration) {
    // Obtener horas, minutos y segundos
    int hours = duration.inHours;
    int minutes = duration.inMinutes.remainder(60);
    int seconds = duration.inSeconds.remainder(60);

    // Formatear cada componente con ceros a la izquierda
    String twoDigits(int n) => n.toString().padLeft(2, '0');

    // Construir la cadena formateada
    return '${twoDigits(hours)}:${twoDigits(minutes)}:${twoDigits(seconds)}';
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
                    'Tiempo anterior ${formatDuration(initialTime)}, seleccione un nuevo tiempo maximo de uso para esta app'),
                TimePickerSpinner(
                  time: DateTime(0, 1, 1, hours, minutes, seconds),
                  is24HourMode: true,
                  isForce2Digits: true,
                  isShowSeconds: true,
                  normalTextStyle: TextStyle(fontSize: 20, color: Colors.black),
                  highlightedTextStyle:
                      TextStyle(fontSize: 24, color: Colors.blue),
                  spacing: 20,
                  onTimeChange: (time) {
                    hours = time.hour;
                    minutes = time.minute;
                    seconds = time.second;
                  },
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
