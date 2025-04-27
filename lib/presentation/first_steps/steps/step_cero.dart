import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:wenia_assignment/presentation/common_widgets/custom_button.dart';
import 'package:wenia_assignment/presentation/first_steps/steps/steps_controller.dart';

class StepCero extends StatelessWidget {
  StepCero({super.key});
  StepsController controller = Get.find();

  @override
  Widget build(BuildContext context) {
    Size sizeScreen = MediaQuery.of(context).size;
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 40),
      child: Column(
        children: [
          SizedBox(height: 30),
          Container(
            alignment: Alignment.center,
            width: sizeScreen.width / 1.5,
            height: sizeScreen.width / 1.5,
            child: Hero(
              tag: 'imageSplash',
              child: Image.asset(
                'assets/icons/logo_productividad.png',
                width: sizeScreen.width / 1.2,
                height: sizeScreen.width / 1.2,
              ),
            ),
          ),
          Expanded(child: Container()),
          Text(
            'Bienvenido',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 30,
              fontFamily: 'paradice',
            ),
          ),
          GestureDetector(
            onTap: () async {
              bool usagePermision = await controller.checkPermisionUsage();
              bool overlayPermision =
                  await controller.checkPermisionOverlay(context);
              print(
                  'usagePermision == $usagePermision === overlayPermision = $overlayPermision');
            },
            child: Container(
              color: Colors.red,
              child: Text('solicitar permiso'),
            ),
          ),
          SizedBox(height: 20),
          Text(
            'podemos ayudarte a maximizar tu productividad, bloquear las aplicaciones que te distraen.',
            textAlign: TextAlign.center,
          ),
          SizedBox(height: 20),
        ],
      ),
    );
  }
}
