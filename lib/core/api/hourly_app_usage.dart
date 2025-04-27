import 'dart:typed_data';
import 'dart:convert';
import 'package:flutter/services.dart';

/// Detalle de eventos dentro de cada lanzamiento
typedef LaunchDetail = _LaunchDetail;

class _LaunchDetail {
  final int eventType;
  final String eventTypeName;
  final DateTime timestamp;
  final String? className;

  _LaunchDetail({
    required this.eventType,
    required this.eventTypeName,
    required this.timestamp,
    this.className,
  });
}

/// Modelo de uso por hora con datos enriquecidos
typedef HourlyAppUsage = _HourlyAppUsage;

class _HourlyAppUsage {
  final String packageName;
  final String appName;
  final Uint8List? icon; // Icono en bytes
  final int hour; // 0–23
  final Duration usage; // Tiempo agregado en pantalla según spans
  final Duration realUsage; // Tiempo real entre open/resume y pause/stop
  final int launches; // Lanzamientos en esa hora
  final int spanCount; // Cantidad de spans
  final Duration totalUsage; // Uso total hoy según spans
  final int totalLaunches; // Lanzamientos totales hoy
  final DateTime firstUse; // Primer uso hoy
  final DateTime lastUse; // Último uso hoy
  final List<_LaunchDetail> launchDetails;

  _HourlyAppUsage({
    required this.packageName,
    required this.appName,
    this.icon,
    required this.hour,
    required this.usage,
    required this.realUsage,
    required this.launches,
    required this.spanCount,
    required this.totalUsage,
    required this.totalLaunches,
    required this.firstUse,
    required this.lastUse,
    required this.launchDetails,
  });
}

/// Servicio que llama al código nativo via MethodChannel
class UsageServiceHourly {
  static const MethodChannel _channel =
      MethodChannel('mi.paquete/usage_hourly');

  /// Función para calcular el tiempo real en pantalla dentro de una hora específica
  static Duration calculateRealUsageForHour(
      int hour, List<_LaunchDetail> details) {
    if (details.isEmpty) return Duration.zero;
    // Asegurarse de que los eventos estén ordenados
    details.sort((a, b) => a.timestamp.compareTo(b.timestamp));
    // Determinar la fecha base a partir del primer evento
    final first = details.first.timestamp;
    final baseDay = DateTime(first.year, first.month, first.day);
    final hourStart = baseDay.add(Duration(hours: hour));
    final hourEnd = hourStart.add(Duration(hours: 1));

    Duration realUsage = Duration.zero;
    DateTime? lastResume;

    for (final detail in details) {
      final ts = detail.timestamp;
      if (detail.eventTypeName == 'ACTIVITY_RESUMED' && lastResume == null) {
        lastResume = ts;
      } else if ((detail.eventTypeName == 'ACTIVITY_PAUSED' ||
              detail.eventTypeName == 'ACTIVITY_STOPPED' ||
              detail.eventTypeName == 'ACTIVITY_DESTROYED') &&
          lastResume != null) {
        // calcular el intervalo resume->pause
        final start = lastResume;
        final end = ts;
        // calcular intersección con la hora objetivo
        final s = start.isAfter(hourStart) ? start : hourStart;
        final e = end.isBefore(hourEnd) ? end : hourEnd;
        if (!e.isBefore(s)) realUsage += e.difference(s);
        lastResume = null;
      }
    }
    return realUsage;
  }

  /// Devuelve la lista completa de uso por hora con detalles
  Future<List<_HourlyAppUsage>> fetchHourlyUsage() async {
    final result =
        await _channel.invokeMethod<List<dynamic>>('getHourlyForegroundUsage');
    if (result == null) return [];

    return result.map((entry) {
      final map = Map<String, dynamic>.from(entry as Map);
      final pkg = map['packageName'] as String;
      final name = map['appName'] as String;
      final iconB64 = map['icon'] as String;
      final hr = map['hour'] as int;
      final ms = map['usage'] as int;
      final launches = map['launches'] as int;
      final spans = map['spanCount'] as int;
      final totalUsageMs = map['totalUsage'] as int;
      final totalLaunches = map['totalLaunches'] as int;
      final firstUseTs = map['firstUse'] as int;
      final lastUseTs = map['lastUse'] as int;

      // Parse detalle de eventos
      final rawDetails = map['launchDetails'] as List<dynamic>? ?? [];
      final launchDetails = rawDetails.map((d) {
        final m = Map<String, dynamic>.from(d as Map);
        return _LaunchDetail(
          eventType: m['eventType'] as int,
          eventTypeName: m['eventTypeName'] as String,
          timestamp: DateTime.fromMillisecondsSinceEpoch(m['timestamp'] as int),
          className: m['className'] as String?,
        );
      }).toList();

      // Calcular realUsage: suma de intervalos resume→pause/stop/destroy
      Duration realUsage = Duration.zero;
      DateTime? lastResume;
      for (final detail in launchDetails) {
        if (detail.eventTypeName == 'ACTIVITY_RESUMED' && lastResume == null) {
          lastResume = detail.timestamp;
        } else if ((detail.eventTypeName == 'ACTIVITY_PAUSED' ||
                detail.eventTypeName == 'ACTIVITY_STOPPED' ||
                detail.eventTypeName == 'ACTIVITY_DESTROYED') &&
            lastResume != null) {
          final delta = detail.timestamp.difference(lastResume);
          if (!delta.isNegative) realUsage += delta;
          lastResume = null;
        }
      }

      Uint8List? iconBytes;
      if (iconB64.isNotEmpty) {
        iconBytes = base64Decode(iconB64);
      }

      return _HourlyAppUsage(
        packageName: pkg,
        appName: name,
        icon: iconBytes,
        hour: hr,
        usage: Duration(milliseconds: ms),
        realUsage: realUsage,
        launches: launches,
        spanCount: spans,
        totalUsage: Duration(milliseconds: totalUsageMs),
        totalLaunches: totalLaunches,
        firstUse: DateTime.fromMillisecondsSinceEpoch(firstUseTs),
        lastUse: DateTime.fromMillisecondsSinceEpoch(lastUseTs),
        launchDetails: launchDetails,
      );
    }).toList();
  }
}
