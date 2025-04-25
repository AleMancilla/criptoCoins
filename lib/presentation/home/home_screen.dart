import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:wenia_assignment/core/api/hourly_usage_chart.dart';
import 'package:wenia_assignment/core/theme/custom_colors.dart';
import 'package:wenia_assignment/core/utils/custom_navigator.dart';
import 'package:wenia_assignment/presentation/home/controller/home_controller.dart';
import 'package:wenia_assignment/presentation/home/list_apps_screen.dart';
import 'package:wenia_assignment/presentation/home/native_comunication_screen.dart';
import 'package:wenia_assignment/presentation/home/widgets/principal_overlay.dart';
import 'package:wenia_assignment/presentation/profile/edit_profile_screen.dart';

class HomeScreen extends StatefulWidget {
  HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  HomeController controller = Get.put(HomeController());

  int _currentIndex = 0;

  final List<Widget> _screens = [
    ListAppsScreen(),
    NativeCommunicationScreen(),
    // UsageTimelinePage(),
    PrincipalOverlay(),
  ];

  void _onTabTapped(int index) {
    setState(() {
      _currentIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
              child: Row(
                children: [
                  Image.asset(
                    'assets/images/logo.png',
                    width: 40,
                    height: 40,
                  ),
                  Expanded(
                      child: Center(
                          child: Text(
                    'Social Stop',
                    style: TextStyle(
                      fontSize: 22,
                      letterSpacing: 5,
                    ),
                  ))),
                  InkWell(
                    onTap: () {
                      CustomNavigator.push(context, EditProfileScreen());
                    },
                    child: Container(
                      decoration: BoxDecoration(
                        color: CustomColors.background3,
                        borderRadius: BorderRadius.circular(10),
                      ),
                      padding: EdgeInsets.all(10),
                      child: Icon(Icons.person),
                    ),
                  ),
                ],
              ),
            ),
            Expanded(child: _screens[_currentIndex]),
          ],
        ),
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: _onTabTapped,
        items: [
          BottomNavigationBarItem(
            icon: Icon(Icons.search),
            label: 'Tablero',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.info),
            label: 'Informes',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.compare_arrows_rounded),
            label: 'Tiempos',
          ),
        ],
      ),
    );
  }
}
