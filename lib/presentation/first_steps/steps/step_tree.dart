import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:installed_apps/index.dart';
import 'package:wenia_assignment/core/theme/custom_colors.dart';
import 'package:wenia_assignment/presentation/first_steps/steps/steps_controller.dart';
import 'package:wenia_assignment/presentation/home/list_apps_controller.dart';

class StepTree extends StatefulWidget {
  StepTree({super.key});

  @override
  State<StepTree> createState() => _StepTreeState();
}

class _StepTreeState extends State<StepTree> with WidgetsBindingObserver {
  StepsController controller = Get.find();

  final ListAppsController listAppscontroller = Get.put(ListAppsController());
  // final ListAppsController listAppscontroller = Get.find();

  final TextEditingController searchController = TextEditingController();

  bool isKeyboardVisible = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeMetrics() {
    final bottomInset = WidgetsBinding.instance.window.viewInsets.bottom;
    setState(() {
      isKeyboardVisible = bottomInset > 0;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Obx(
        () {
          return Column(
            children: [
              Container(
                height: 60,
                padding: EdgeInsets.symmetric(horizontal: 15, vertical: 5),
                child: TextField(
                  controller: searchController,
                  decoration: InputDecoration(
                    contentPadding:
                        EdgeInsets.symmetric(horizontal: 10, vertical: 0),
                    labelText: 'Buscar aplicación',
                    prefixIcon: Icon(Icons.search),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                    ),
                  ),
                  onChanged: (value) {
                    listAppscontroller.filterApps(value);
                  },
                ),
              ),
              Expanded(
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  child: Scrollbar(
                    thumbVisibility: true,
                    thickness: 5,
                    child: ListView.builder(
                      itemCount: listAppscontroller.filteredApps.length,
                      padding: EdgeInsets.all(0),
                      itemBuilder: (context, index) {
                        AppInfo app = listAppscontroller.filteredApps[index];
                        Duration? usageTime =
                            listAppscontroller.appUsageStats[app.packageName];

                        bool isSelected = listAppscontroller.appsSelectable
                            .contains(app.packageName);

                        return Opacity(
                          opacity: isSelected ? 1 : 0.5,
                          child: Container(
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(10),
                              color: Colors.white,
                              boxShadow: [
                                BoxShadow(
                                  blurRadius: 5,
                                  spreadRadius: 2,
                                  color: Colors.black12,
                                ),
                              ],
                            ),
                            margin: const EdgeInsets.only(
                                bottom: 5, left: 5, right: 5, top: 2),
                            child: ListTile(
                              contentPadding: const EdgeInsets.symmetric(
                                  horizontal: 10, vertical: 0),
                              dense: true,
                              onTap: () {
                                listAppscontroller.selectApp(app.packageName);
                                setState(() {});
                              },
                              trailing: Icon(
                                isSelected
                                    ? Icons.check_box
                                    : Icons.check_box_outline_blank,
                                color: isSelected
                                    ? CustomColors.primary3
                                    : CustomColors.background3,
                              ),
                              leading: (app.icon != null)
                                  ? Image.memory(
                                      app.icon!,
                                      width: 40,
                                      height: 40,
                                    )
                                  : const Icon(Icons.android),
                              title: Text(app.name),
                              subtitle: Text(
                                'Hoy lo usaste: ${listAppscontroller.formatDuration(usageTime ?? Duration())}',
                                style: TextStyle(fontSize: 12),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ),
              ),
              if (!isKeyboardVisible)
                Padding(
                  padding: EdgeInsets.symmetric(horizontal: 40),
                  child: Column(
                    children: [
                      Text(
                        'Principales redes sociales',
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
                    ],
                  ),
                ),
            ],
          );
        },
      ),
    );
  }
}
