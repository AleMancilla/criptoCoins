import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:wenia_assignment/core/api/hourly_app_usage.dart';

class HourlyUsagePage extends StatefulWidget {
  @override
  _HourlyUsagePageState createState() => _HourlyUsagePageState();
}

class _HourlyUsagePageState extends State<HourlyUsagePage> {
  List<HourlyAppUsage> _data = [];

  @override
  void initState() {
    super.initState();
    UsageService().fetchHourlyUsage().then((list) {
      setState(() => _data = list);
    });
  }

  @override
  Widget build(BuildContext context) {
    // Agrupar por hora
    final byHour = <int, List<HourlyAppUsage>>{};
    for (var u in _data) {
      byHour.putIfAbsent(u.hour, () => []).add(u);
    }

    return ListView(
      children: byHour.entries.map((e) {
        final hour = e.key;
        final items = e.value;
        return ExpansionTile(
          title: Text('Hora $hour:00 – ${hour + 1}:00'),
          children: items.map((u) {
            return ListTile(
              title: Text(u.packageName),
              subtitle:
                  Text('${u.usage.inMinutes} min en ${u.launches} aperturas'),
            );
          }).toList(),
        );
      }).toList(),
    );
  }
}
