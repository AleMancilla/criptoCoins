import 'package:flutter/material.dart';
import 'package:wenia_assignment/core/api/UsageTimelinePage.dart';

class UsageChartPainter extends CustomPainter {
  final Map<String, List<ChartData>> series;

  UsageChartPainter(this.series);

  @override
  void paint(Canvas canvas, Size size) {
    // Márgenes para ejes y etiquetas
    final left = 40.0, top = 20.0, right = 20.0, bottom = 40.0;
    final chartWidth = size.width - left - right;
    final chartHeight = size.height - top - bottom;

    // Encuentra el valor máximo de minutos para escalar
    double maxY = 0;
    series.values.forEach((list) {
      list.forEach((d) {
        if (d.minutes > maxY) maxY = d.minutes;
      });
    });

    final paintAxis = Paint()
      ..color = Colors.black
      ..strokeWidth = 1;
    final paintGrid = Paint()
      ..color = Colors.grey.withOpacity(0.5)
      ..strokeWidth = 0.5;
    final textPainter = TextPainter(textDirection: TextDirection.ltr);

    // Dibujar ejes
    final origin = Offset(left, top + chartHeight);
    final xEnd = Offset(left + chartWidth, top + chartHeight);
    final yEnd = Offset(left, top);
    canvas.drawLine(origin, xEnd, paintAxis);
    canvas.drawLine(origin, yEnd, paintAxis);

    // Líneas horizontales y labels Y
    const int ySteps = 5;
    for (int i = 0; i <= ySteps; i++) {
      final y = top + chartHeight * (1 - i / ySteps);
      canvas.drawLine(Offset(left, y), Offset(left + chartWidth, y), paintGrid);

      final label = (maxY * (i / ySteps)).round().toString();
      textPainter.text = TextSpan(
          text: label, style: TextStyle(color: Colors.black, fontSize: 10));
      textPainter.layout();
      textPainter.paint(canvas,
          Offset(left - textPainter.width - 4, y - textPainter.height / 2));
    }

    // Ticks y labels X (cada 4 horas)
    for (int h = 0; h < 24; h++) {
      if (h % 4 == 0) {
        final x = left + chartWidth * (h / 23);
        canvas.drawLine(Offset(x, top + chartHeight),
            Offset(x, top + chartHeight + 5), paintAxis);

        final label = '${h.toString().padLeft(2, '0')}:00';
        textPainter.text = TextSpan(
            text: label, style: TextStyle(color: Colors.black, fontSize: 10));
        textPainter.layout();
        textPainter.paint(
            canvas, Offset(x - textPainter.width / 2, top + chartHeight + 6));
      }
    }

    // Dibujar cada serie (línea + puntos)
    final colors = [
      Colors.red,
      Colors.blue,
      Colors.green,
      Colors.orange,
      Colors.purple,
      Colors.brown
    ];
    int colorIdx = 0;

    series.forEach((pkg, list) {
      // Ruta de la línea
      final path = Path();
      for (int i = 0; i < list.length; i++) {
        final d = list[i];
        final x = left + chartWidth * (d.hour / 23);
        final y = top + chartHeight * (1 - (maxY > 0 ? d.minutes / maxY : 0));
        if (i == 0) {
          path.moveTo(x, y);
        } else {
          path.lineTo(x, y);
        }
      }

      final paintLine = Paint()
        ..color = colors[colorIdx % colors.length]
        ..strokeWidth = 2
        ..style = PaintingStyle.stroke;
      canvas.drawPath(path, paintLine);

      // Dibujar puntos
      final paintDot = Paint()..color = colors[colorIdx % colors.length];
      for (var d in list) {
        final x = left + chartWidth * (d.hour / 23);
        final y = top + chartHeight * (1 - (maxY > 0 ? d.minutes / maxY : 0));
        canvas.drawCircle(Offset(x, y), 3, paintDot);
      }

      colorIdx++;
    });
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
