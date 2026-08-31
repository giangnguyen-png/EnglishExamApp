import 'package:flutter/material.dart';

import '../config/app_colors.dart';

class AccentCard extends StatelessWidget {
  final Color color;
  final Widget child;
  final EdgeInsetsGeometry padding;
  final EdgeInsetsGeometry? margin;

  const AccentCard({
    super.key,
    required this.color,
    required this.child,
    this.padding = const EdgeInsets.all(16),
    this.margin,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: margin,
      color: AppColors.soft(color),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: color.withOpacity(0.35)),
      ),
      child: Padding(padding: padding, child: child),
    );
  }
}
