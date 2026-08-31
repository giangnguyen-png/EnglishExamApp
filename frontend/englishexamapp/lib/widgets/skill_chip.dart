import 'package:flutter/material.dart';

import '../config/app_colors.dart';

class SkillChip extends StatelessWidget {
  final String skillType;
  final String? label;

  const SkillChip({
    super.key,
    required this.skillType,
    this.label,
  });

  @override
  Widget build(BuildContext context) {
    final color = AppColors.skill(skillType);
    return Chip(
      avatar: Icon(_skillIcon(skillType), color: color, size: 18),
      label: Text(label ?? _skillLabel(skillType)),
      backgroundColor: AppColors.soft(color),
      side: BorderSide(color: color.withOpacity(0.32)),
    );
  }
}

IconData _skillIcon(String skillType) {
  switch (skillType) {
    case 'LISTENING':
      return Icons.headphones;
    case 'READING':
      return Icons.menu_book;
    case 'WRITING':
      return Icons.edit_note;
    case 'SPEAKING':
      return Icons.mic;
    default:
      return Icons.school;
  }
}

String _skillLabel(String skillType) {
  switch (skillType) {
    case 'LISTENING':
      return 'Listening';
    case 'READING':
      return 'Reading';
    case 'WRITING':
      return 'Writing';
    case 'SPEAKING':
      return 'Speaking';
    default:
      return skillType;
  }
}
