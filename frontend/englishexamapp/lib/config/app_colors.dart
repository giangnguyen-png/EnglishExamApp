import 'package:flutter/material.dart';

class AppColors {
  static const primary = Color(0xFF4F5FA8);
  static const listening = Color(0xFF2563EB);
  static const reading = Color(0xFF16A34A);
  static const writing = Color(0xFFF97316);
  static const speaking = Color(0xFF7C3AED);
  static const premium = Color(0xFFD97706);
  static const expert = Color(0xFF0F766E);
  static const success = Color(0xFF16A34A);
  static const warning = Color(0xFFD97706);
  static const error = Color(0xFFDC2626);

  static Color skill(String skillType) {
    switch (skillType) {
      case 'LISTENING':
        return listening;
      case 'READING':
        return reading;
      case 'WRITING':
        return writing;
      case 'SPEAKING':
        return speaking;
      default:
        return primary;
    }
  }

  static Color soft(Color color) {
    return Color.alphaBlend(color.withOpacity(0.10), Colors.white);
  }
}
