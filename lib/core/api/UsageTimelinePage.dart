import 'package:flutter/material.dart';

import 'package:wenia_assignment/core/api/hourly_app_usage.dart';

/// Punto de datos para el gráfico
class ChartData {
  final int hour; // 0–23
  final double minutes; // minutos usados en esa hora
  ChartData(this.hour, this.minutes);
}

class UsagePageSelectable extends StatefulWidget {
  Map<int, List<HourlyAppUsage>> listHourlyAppUsage;
  UsagePageSelectable(this.listHourlyAppUsage);
  @override
  _UsagePageSelectableState createState() => _UsagePageSelectableState();
}

class _UsagePageSelectableState extends State<UsagePageSelectable> {
  int? _selectedHour;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // El área del gráfico
        SizedBox(
          height: 200,
          child: Padding(
            padding: const EdgeInsets.all(12.0),
            child: GestureDetector(
              onTapDown: _onTapDown,
              child: CustomPaint(
                painter: _StackedBarPainter(
                  data: widget.listHourlyAppUsage,
                  selectedHour: _selectedHour,
                ),
                child: Container(),
              ),
            ),
          ),
        ),
        // Detalle de la selección
        if (_selectedHour != null)
          Container(
            color: Colors.grey.shade100,
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Detalles hora ${_selectedHour}:00 – ${_selectedHour! + 1}:00',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                ..._buildDetailRows(_selectedHour!),
              ],
            ),
          ),
      ],
    );
  }

  void _onTapDown(TapDownDetails details) {
    final box = context.findRenderObject() as RenderBox;
    final local = box.globalToLocal(details.globalPosition);

    // Márgenes idénticos a los del painter
    const leftMargin = 0.0;
    const bottomMargin = 10.0;
    const topMargin = 10.0;
    const rightMargin = 0.0;

    final chartWidth = box.size.width - leftMargin - rightMargin;
    // Sólo si tocan dentro del área de barras:
    if (local.dx < leftMargin || local.dx > leftMargin + chartWidth) return;

    // Calcula la hora tocada
    double relativeX = local.dx - leftMargin;
    int hour = (relativeX / chartWidth * 24).floor();
    hour = hour.clamp(0, 23);

    setState(() {
      _selectedHour = hour;
    });
  }

  List<Widget> _buildDetailRows(int hour) {
    List<HourlyAppUsage> map = widget.listHourlyAppUsage[hour] ?? [];
    if (map.isEmpty) {
      return [Text('Sin uso registrado')];
    }
    return map.map((e) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Row(
          children: [
            Image.memory(
              e.icon!,
              width: 40,
              height: 40,
            ),
            const SizedBox(width: 8),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('${e.appName}: ${timeToUse(e.usage)}'),
                Text(
                  '${e.launches} interacciones',
                  style: TextStyle(fontSize: 10, color: Colors.grey),
                ),
              ],
            ),
          ],
        ),
      );
    }).toList();
  }

  String timeToUse(Duration usage) {
    if (usage.inMinutes == 0) {
      return 'Menos de 1 minuto';
    }
    return '${usage.inMinutes.round()} minutos';
  }
}

class _StackedBarPainter extends CustomPainter {
  final Map<int, List<HourlyAppUsage>> data;
  final int? selectedHour;

  _StackedBarPainter({
    required this.data,
    this.selectedHour,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint();
    final textPainter = TextPainter(textDirection: TextDirection.ltr);

    // Márgenes y dimensiones
    const leftMargin = 0.0;
    const bottomMargin = 10.0;
    const topMargin = 10.0;
    const rightMargin = 0.0;
    final chartWidth = size.width - leftMargin - rightMargin;
    final chartHeight = size.height - topMargin - bottomMargin;

    // Ejes
    final origin = Offset(leftMargin, topMargin + chartHeight);
    final xEnd = Offset(leftMargin + chartWidth, origin.dy);
    final yEnd = Offset(leftMargin, topMargin);
    paint
      ..color = Colors.black
      ..strokeWidth = 1;
    canvas.drawLine(origin, xEnd, paint);
    canvas.drawLine(origin, yEnd, paint);

    // --- 1) Escalar en Y ---
    double maxTotal = 0;
    data.values.forEach((list) {
      final sum = list.fold<double>(0, (p, e) => p + e.usage.inMinutes);
      if (sum > maxTotal) maxTotal = sum;
    });

    // --- 2) Líneas de grid Y + labels ---
    const ySteps = 4;
    for (int i = 0; i <= ySteps; i++) {
      final y = topMargin + chartHeight * (1 - i / ySteps);
      paint
        ..color = Colors.red.withOpacity(0.4)
        ..strokeWidth = 0.5;
      canvas.drawLine(
          Offset(leftMargin, y), Offset(leftMargin + chartWidth, y), paint);

      textPainter.text = TextSpan(
        text: ((maxTotal * i / ySteps).round()).toString(),
        style: TextStyle(color: Colors.black, fontSize: 10),
      );
      textPainter.layout();
      textPainter.paint(
        canvas,
        Offset(leftMargin - textPainter.width - 5, y - textPainter.height / 2),
      );
    }

    // --- 3) Labels X ---
    const xLabels = [0, 6, 12, 18, 23];
    for (var h in xLabels) {
      final x = leftMargin + chartWidth * (h / 23);
      paint
        ..color = Colors.black
        ..strokeWidth = 1;
      canvas.drawLine(Offset(x, origin.dy), Offset(x, origin.dy + 5), paint);

      textPainter.text = TextSpan(
        text: '$h',
        style: TextStyle(color: Colors.black, fontSize: 10),
      );
      textPainter.layout();
      textPainter.paint(
          canvas, Offset(x - textPainter.width / 2, origin.dy + 5));
    }

    // --- 4) Parámetros de barras ---
    final barUnit = chartWidth / 24;
    final barWidth = barUnit * 0.9;
    final gap = (barUnit - barWidth) / 2;

    // --- 5) Dibujar cada barra apilada con clip para curva superior ---
    data.forEach((hour, segments) {
      final xHour = leftMargin + barUnit * hour + gap;

      // 5.1) Sombra de selección
      if (selectedHour == hour) {
        paint
          ..color = Colors.yellow.withOpacity(0.2)
          ..style = PaintingStyle.fill;
        canvas.drawRect(
          Rect.fromLTWH(
              xHour - gap, topMargin, barWidth + gap * 2, chartHeight),
          paint,
        );
      }

      // 5.2) Color base
      final baseColor = (selectedHour == hour)
          ? Colors.blue.shade800
          : Colors.blue.withOpacity(0.5);
      paint.style = PaintingStyle.fill;

      // 5.3) Calcular altura total
      final totalHeight = segments.fold<double>(0, (sum, pkg) {
        final h =
            maxTotal > 0 ? (pkg.usage.inMinutes / maxTotal) * chartHeight : 0;
        return sum + h;
      });

      // 5.4) Definir RRect exterior con esquinas curvas arriba
      final fullBarRect = Rect.fromLTWH(
        xHour,
        origin.dy - totalHeight,
        barWidth,
        totalHeight,
      );
      final outerRRect = RRect.fromRectAndCorners(
        fullBarRect,
        topLeft: Radius.circular(6),
        topRight: Radius.circular(6),
      );

      // 5.5) Clip y dibujar segmentos dentro
      canvas.save();
      canvas.clipRRect(outerRRect);

      double yAccum = origin.dy;
      for (var pkg in segments) {
        final segH =
            maxTotal > 0 ? (pkg.usage.inMinutes / maxTotal) * chartHeight : 0;
        final segRect =
            Rect.fromLTWH(xHour, yAccum - segH, barWidth, segH.toDouble());
        paint.color = baseColor;
        canvas.drawRect(segRect, paint);
        yAccum -= segH;
      }
      canvas.restore();

      // (Opcional) si quieres un contorno
      // paint
      //   ..style = PaintingStyle.stroke
      //   ..color = baseColor
      //   ..strokeWidth = 1;
      // canvas.drawRRect(outerRRect, paint);
    });
  }

  @override
  bool shouldRepaint(covariant _StackedBarPainter old) {
    return old.data != data || old.selectedHour != selectedHour;
  }
}
