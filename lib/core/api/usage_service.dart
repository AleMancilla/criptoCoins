import 'package:flutter/services.dart';

/// Modelo para representar el uso de una app.
class AppUsage {
  final String packageName;
  final Duration usage;

  AppUsage({
    required this.packageName,
    required this.usage,
  });
}

/// Servicio que habla con el canal nativo y te entrega objetos AppUsage.
class UsageService {
  static const _channel = MethodChannel('mi.paquete/usage');

  /// Devuelve un listado de AppUsage con el uso de hoy.
  static Future<List<AppUsage>> fetchUsageToday() async {
    final Map<dynamic, dynamic>? raw =
        await _channel.invokeMapMethod('getUsageToday');
    if (raw == null) return [];

    return raw.entries.map((e) {
      final pkg = e.key as String;
      final ms = e.value as int;
      return AppUsage(
        packageName: pkg,
        usage: Duration(milliseconds: ms),
      );
    }).toList();
  }
}
