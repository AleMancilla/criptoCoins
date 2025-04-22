import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:wenia_assignment/presentation/first_steps/first_steps_screen.dart';
import 'package:wenia_assignment/presentation/home/home_screen.dart';
import 'package:wenia_assignment/presentation/home/list_apps_controller.dart';
import 'package:wenia_assignment/presentation/widgets/scaffold_background.dart';
import '/presentation/auth/auth_home_screen.dart';
import '/core/theme/custom_colors.dart';
import '/core/utils/custom_navigator.dart';
import '/core/utils/user_preferens.dart';

class SplashScreen extends StatefulWidget {
  @override
  _SplashScreenState createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen>
    with SingleTickerProviderStateMixin {
  AnimationController? _controller;
  Animation<double>? _animation;
  final prefs = UserPreferences();
  // final ListAppsController listAppscontroller = Get.put(ListAppsController());

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    );
    _animation = CurvedAnimation(parent: _controller!, curve: Curves.easeIn);

    _controller!.forward();

    _loadDataAndNavigate();
  }

  Future<void> _loadDataAndNavigate() async {
    await Future.delayed(Duration(seconds: 5)); // Simula carga de datos
    prefs.printUserData();
    print(
        ' ===== >>> ${prefs.useruid} -- ${prefs.useruid == null} -- ${prefs.useruid == ''}');
    print(' ===== >>> ${!(prefs.useruid == null || prefs.useruid == '')}');

    if (!(prefs.useruid == null || prefs.useruid == '')) {
      CustomNavigator.pushReplacement(context, HomeScreen());
    } else {
      // CustomNavigator.pushReplacement(context, AuthHomeScreen());
      // CustomNavigator.pushReplacement(context, HomeScreen());
      CustomNavigator.pushReplacement(context, FirstStepsScreen());
    }
  }

  @override
  void dispose() {
    _controller!.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    Size sizeScreen = MediaQuery.of(context).size;
    return ScaffoldBackground(
      body: Column(
        children: [
          Expanded(
            child: FadeTransition(
              opacity: _animation!,
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
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
                    Text(
                      'Social Stop',
                      style: TextStyle(
                          color: Colors.white,
                          fontSize: 40,
                          fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
              ),
            ),
          ),
          SizedBox(height: 40)
        ],
      ),
    );
  }
}
