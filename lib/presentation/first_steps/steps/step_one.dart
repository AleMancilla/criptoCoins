import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:wenia_assignment/presentation/common_widgets/custom_button.dart';
import 'package:wenia_assignment/presentation/first_steps/steps/steps_controller.dart';

class StepOne extends StatelessWidget {
  StepOne({super.key});
  StepsController controller = Get.find();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 40),
      child: Obx(
        () => Column(
          children: [
            Expanded(child: Container()),
            Text(
              'Permiso de uso requerido',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 24,
                fontFamily: 'paradice',
              ),
            ),
            SizedBox(height: 20),
            Text(
              'Social Stop necesita permiso para el uso de la API para poder accerder a tu información de uso. Por favor selecciona dar permiso, luego selecciona Social Media de la lista de aplicaciones y habilita el permiso',
              textAlign: TextAlign.center,
            ),
            SizedBox(height: 20),
            controller.permisionUsage.value
                ? CustomButton(
                    text: 'Permiso otorgado',
                    color: Colors.green,
                  )
                : CustomButton(
                    text: 'Dar permiso',
                    ontap: () async {
                      await controller.openSettings();
                    },
                  ),
            SizedBox(height: 20),
          ],
        ),
      ),
    );
  }
}
