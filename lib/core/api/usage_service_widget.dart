import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:wenia_assignment/core/api/usage_service.dart';

class UsageServiceWidget extends StatefulWidget {
  const UsageServiceWidget({super.key});

  @override
  State<UsageServiceWidget> createState() => _UsageServiceWidgetState();
}

class _UsageServiceWidgetState extends State<UsageServiceWidget> {
  List<AppUsage> _usageList = [];

  @override
  void initState() {
    super.initState();
    _loadUsage();
  }

  Future<void> _loadUsage() async {
    final list = await UsageService.fetchUsageToday();
    setState(() => _usageList = list);
  }

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      itemCount: _usageList.length,
      itemBuilder: (context, i) {
        final info = _usageList[i];
        return ListTile(
          title: Text(info.packageName),
          subtitle: Text('${info.usage.inMinutes} minutos'),
        );
      },
    );
  }
}
