import 'dart:convert';
import 'dart:typed_data';
import 'package:flutter/services.dart';

class HourlyAppUsage {
  final String packageName;
  final String appName;
  final Uint8List? icon; // null si no venía
  final int hour; // 0–23
  final Duration usage;
  final int launches;

  HourlyAppUsage({
    required this.packageName,
    required this.appName,
    this.icon,
    required this.hour,
    required this.usage,
    required this.launches,
  });
}

class UsageServiceHourly {
  static const _chan = MethodChannel('mi.paquete/usage_hourly');

  Future<List<HourlyAppUsage>> fetchHourlyUsage() async {
    final List<dynamic>? raw =
        await _chan.invokeMethod<List<dynamic>>('getHourlyForegroundUsage');
    if (raw == null) return [];
    return raw.map((entry) {
      final map = entry as Map<dynamic, dynamic>;
      final pkg = map['packageName'] as String;
      final name = map['appName'] as String;
      final iconB64 = map['icon'] as String;
      final hr = map['hour'] as int;
      final ms = map['usage'] as int;
      final cnt = map['launches'] as int;

      Uint8List? iconBytes;
      if (iconB64.isNotEmpty) {
        iconBytes = base64Decode(iconB64);
      }

      return HourlyAppUsage(
        packageName: pkg,
        appName: name,
        icon: iconBytes,
        hour: hr,
        usage: Duration(milliseconds: ms),
        launches: cnt,
      );
    }).toList();
  }
}
