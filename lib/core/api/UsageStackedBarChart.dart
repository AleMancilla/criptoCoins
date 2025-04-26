import 'package:flutter/material.dart';
import 'dart:math';

/// Datos de ejemplo: para cada hora (0–23) un mapa appName→minutos.
final Map<int, Map<String, double>> sampleData = {
  0: {'Facebook': 35, 'Instagram': 10, 'YouTube': 0},
  1: {'Facebook': 10, 'Instagram': 5, 'YouTube': 20},
  2: {'Facebook': 0, 'Instagram': 0, 'YouTube': 0},
  // …
  12: {'Facebook': 60, 'Instagram': 30, 'YouTube': 45},
  13: {'Facebook': 30, 'Instagram': 5, 'YouTube': 15},
  // Completa para todas las horas que necesites…
  18: {'Facebook': 90, 'Instagram': 45, 'YouTube': 30},
  19: {'Facebook': 45, 'Instagram': 20, 'YouTube': 10},
  // …
  23: {'Facebook': 5, 'Instagram': 0, 'YouTube': 0},
};

/// Colores asignados a cada app
final Map<String, Color> appColors = {
  'Facebook': Colors.blue.shade800,
  'Instagram': Colors.pinkAccent,
  'YouTube': Colors.redAccent,
};

class UsageStackedBarChart extends StatelessWidget {
  final Map<int, Map<String, double>> data;
  final Map<String, Color> colors;

  const UsageStackedBarChart({
    Key? key,
    required this.data,
    required this.colors,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return AspectRatio(
      aspectRatio: 1.6,
      child: CustomPaint(
        painter: _StackedBarPainter(data: data, colors: colors),
        child: Container(),
      ),
    );
  }
}

class _StackedBarPainter extends CustomPainter {
  final Map<int, Map<String, double>> data;
  final Map<String, Color> colors;

  _StackedBarPainter({required this.data, required this.colors});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint();
    final textPainter = TextPainter(
      textDirection: TextDirection.ltr,
      textAlign: TextAlign.center,
    );

    // Márgenes
    const leftMargin = 40.0;
    const bottomMargin = 30.0;
    const topMargin = 20.0;
    const rightMargin = 20.0;

    final chartWidth = size.width - leftMargin - rightMargin;
    final chartHeight = size.height - topMargin - bottomMargin;

    // Encuentra el máximo total de minutos en una sola hora
    double maxTotal = 0;
    data.values.forEach((map) {
      final sum = map.values.fold(0.0, (a, b) => a + b);
      if (sum > maxTotal) maxTotal = sum;
    });

    // Ejes X e Y
    final origin = Offset(leftMargin, topMargin + chartHeight);
    final xEnd = Offset(leftMargin + chartWidth, topMargin + chartHeight);
    final yEnd = Offset(leftMargin, topMargin);

    paint
      ..color = Colors.black
      ..strokeWidth = 1;
    canvas.drawLine(origin, xEnd, paint);
    canvas.drawLine(origin, yEnd, paint);

    // Etiquetas Y (0%, 25%, 50%, 75%, 100%)
    const ySteps = 4;
    for (int i = 0; i <= ySteps; i++) {
      final y = topMargin + chartHeight * (1 - i / ySteps);
      // línea de grid
      paint
        ..color = Colors.grey.withOpacity(0.4)
        ..strokeWidth = 0.5;
      canvas.drawLine(
          Offset(leftMargin, y), Offset(leftMargin + chartWidth, y), paint);

      // etiqueta
      final label = ((maxTotal * i / ySteps).round()).toString();
      textPainter.text = TextSpan(
        text: label,
        style: TextStyle(color: Colors.black, fontSize: 10),
      );
      textPainter.layout();
      textPainter.paint(
        canvas,
        Offset(leftMargin - textPainter.width - 6, y - textPainter.height / 2),
      );
    }

    // Etiquetas X (0, 6, 12, 18, 23)
    const xLabels = [0, 6, 12, 18, 23];
    for (var h in xLabels) {
      final x = leftMargin + chartWidth * (h / 23);
      // tick
      paint
        ..color = Colors.black
        ..strokeWidth = 1;
      canvas.drawLine(Offset(x, origin.dy), Offset(x, origin.dy + 5), paint);

      // label
      textPainter.text = TextSpan(
        text: h.toString(),
        style: TextStyle(color: Colors.black, fontSize: 10),
      );
      textPainter.layout();
      textPainter.paint(
        canvas,
        Offset(x - textPainter.width / 2, origin.dy + 6),
      );
    }

    // Ancho de cada barra
    final barWidth = chartWidth / 24 * 0.6;
    final gap = (chartWidth / 24 - barWidth) / 2;

    // Dibujar barras apiladas
    data.forEach((hour, usageMap) {
      final xHour = leftMargin + (chartWidth / 24) * hour + gap;
      double yAccum = origin.dy;

      // Ordena las apps para consistencia (o define tu propio orden)
      usageMap.forEach((pkg, mins) {
        final height = (mins / maxTotal) * chartHeight;
        paint.color = colors[pkg] ?? Colors.grey;

        final rect = Rect.fromLTWH(
          xHour,
          yAccum - height,
          barWidth,
          height,
        );
        canvas.drawRect(rect, paint);

        yAccum -= height;
      });
    });
  }

  @override
  bool shouldRepaint(covariant _StackedBarPainter old) {
    return old.data != data || old.colors != colors;
  }
}
