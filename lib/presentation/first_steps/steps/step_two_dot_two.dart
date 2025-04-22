// import 'package:flutter/material.dart';
// import 'package:get/get.dart';
// import 'package:wenia_assignment/presentation/common_widgets/custom_button.dart';
// import 'package:wenia_assignment/presentation/first_steps/steps/steps_controller.dart';

// class StepTwoDotTwo extends StatelessWidget {
//   StepTwoDotTwo({super.key});
//   StepsController controller = Get.find();

//   @override
//   Widget build(BuildContext context) {
//     return Container(
//       padding: EdgeInsets.symmetric(horizontal: 40),
//       child: Obx(
//         () => Column(
//           children: [
//             Expanded(child: Container()),
//             Text(
//               'Permiso de Accesibilidad',
//               style: TextStyle(
//                 fontWeight: FontWeight.bold,
//                 fontSize: 24,
//                 fontFamily: 'paradice',
//               ),
//             ),
//             SizedBox(height: 20),
//             Text(
//               'Social Stop necesita permiso para que la app funcione correctamente',
//               textAlign: TextAlign.center,
//             ),
//             SizedBox(height: 20),
//             controller.permisionAccesibility.value
//                 ? CustomButton(
//                     text: 'Permiso otorgado',
//                     color: Colors.green,
//                   )
//                 : CustomButton(
//                     text: 'Dar permiso',
//                     ontap: () async {
//                       // await controller.askForSuperpositionPermision();
//                       await controller.permisionAccesibilidad(context);
//                     },
//                   ),
//             SizedBox(height: 20),
//           ],
//         ),
//       ),
//     );
//   }
// }
