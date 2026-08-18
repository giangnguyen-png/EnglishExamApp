import 'package:flutter/material.dart';

import '../../models/result.dart';

class ResultScreen extends StatelessWidget {
  final AttemptResult result;

  const ResultScreen({super.key, required this.result});

  @override
  Widget build(BuildContext context) {
    const skillOrder = ['LISTENING', 'READING', 'WRITING', 'SPEAKING'];

    return Scaffold(
      appBar: AppBar(title: const Text('Kết quả')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            result.examTitle,
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  const Text('Overall Band'),
                  const SizedBox(height: 8),
                  Text(
                    _formatBand(result.overallBandScore),
                    style: Theme.of(context).textTheme.displaySmall,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          ...skillOrder.map((skillType) {
            final skill = result.skillByType(skillType);
            return _SkillResultCard(
              title: _skillLabel(skillType),
              skillType: skillType,
              bandScore: skill?.bandScore,
              aiAnalysis: skill?.aiAnalysis ?? '',
            );
          }),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Đánh giá tổng quan',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    result.aiOverallFeedback.isEmpty
                        ? 'Chưa có đánh giá tổng quan.'
                        : result.aiOverallFeedback,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SkillResultCard extends StatelessWidget {
  final String title;
  final String skillType;
  final double? bandScore;
  final String aiAnalysis;

  const _SkillResultCard({
    required this.title,
    required this.skillType,
    required this.bandScore,
    required this.aiAnalysis,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                Text(
                  _formatSkillBand(skillType, bandScore),
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ],
            ),
            if (aiAnalysis.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(aiAnalysis),
            ],
          ],
        ),
      ),
    );
  }
}

String _formatSkillBand(String skillType, double? value) {
  if (value == null && skillType == 'SPEAKING') {
    return 'Chờ giám khảo';
  }
  return _formatBand(value);
}

String _formatBand(double? value) {
  if (value == null) {
    return 'Chưa có điểm';
  }
  return value.toStringAsFixed(1);
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
