import 'package:flutter/services.dart';

class HourlyAppUsage {
  final String packageName;
  final int hour; // 0–23
  final Duration usage;
  final int launches;

  HourlyAppUsage({
    required this.packageName,
    required this.hour,
    required this.usage,
    required this.launches,
  });
}

class UsageService {
  static const _chan = MethodChannel('mi.paquete/usage_hourly');

  /// Devuelve una lista de HourlyAppUsage, sin ordenar.
  Future<List<HourlyAppUsage>> fetchHourlyUsage() async {
    final Map<dynamic, dynamic>? raw =
        await _chan.invokeMapMethod('getHourlyUsage');
    if (raw == null) return [];

    final List<HourlyAppUsage> out = [];

    raw.forEach((pkg, perHour) {
      final hoursMap = perHour as Map<dynamic, dynamic>;
      hoursMap.forEach((hrStr, data) {
        final hour = int.parse(hrStr.toString());
        final list = data as List<dynamic>;
        final ms = list[0] as int;
        final cnt = list[1] as int;
        out.add(HourlyAppUsage(
          packageName: pkg as String,
          hour: hour,
          usage: Duration(milliseconds: ms),
          launches: cnt,
        ));
      });
    });

    return out;
  }
}
