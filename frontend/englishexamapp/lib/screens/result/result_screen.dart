import 'package:flutter/material.dart';

import '../../config/app_colors.dart';
import '../../models/result.dart';
import '../../widgets/accent_card.dart';
import '../review/attempt_review_screen.dart';

class ResultScreen extends StatelessWidget {
  final AttemptResult result;

  const ResultScreen({super.key, required this.result});

  @override
  Widget build(BuildContext context) {
    const skillOrder = ['LISTENING', 'READING', 'WRITING', 'SPEAKING'];
    final speaking = result.skillByType('SPEAKING');
    final waitingForExpert = speaking != null && speaking.bandScore == null;
    final canReviewObjective = result.normalAttempt &&
        result.endTime.isNotEmpty &&
        (result.skillByType('LISTENING') != null ||
            result.skillByType('READING') != null);

    return Scaffold(
      appBar: AppBar(title: const Text('Kết quả')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Center(
            child: Text(
              'IELTS Practice Result',
              style: Theme.of(context).textTheme.titleLarge,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            result.examTitle,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 16),
          AccentCard(
            color: AppColors.primary,
            child: Column(
              children: [
                const Text('Overall Band'),
                const SizedBox(height: 8),
                Text(
                  result.overallBandScore == null
                      ? 'Chưa có kết quả'
                      : _formatBand(result.overallBandScore),
                  style: Theme.of(context)
                      .textTheme
                      .displaySmall
                      ?.copyWith(color: AppColors.primary),
                  textAlign: TextAlign.center,
                ),
              ],
              ),
          ),
          if (canReviewObjective) ...[
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) => AttemptReviewScreen(
                      attemptId: result.attemptId,
                    ),
                  ),
                );
              },
              icon: const Icon(Icons.rate_review_outlined),
              label: const Text('Xem lại bài làm'),
            ),
          ],
          if (waitingForExpert) ...[
            const SizedBox(height: 12),
            const AccentCard(
              color: AppColors.premium,
              child: Text(
                  'Speaking đang chờ giám khảo chấm.\nKết quả tổng sẽ được cập nhật sau.',
              ),
            ),
          ],
          const SizedBox(height: 16),
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1.28,
            children: skillOrder.map((skillType) {
              final skill = result.skillByType(skillType);
              return _SkillResultCard(
                title: _skillLabel(skillType),
                skillType: skillType,
                bandScore: skill?.bandScore,
              );
            }).toList(),
          ),
          _buildSkillAnalysis(context, result),
          const SizedBox(height: 12),
          _FeedbackCard(
            title: 'Đánh giá tổng quan',
            feedback: result.overallFeedback,
            emptyMessage: 'Chưa có đánh giá tổng quan.',
          ),
        ],
      ),
    );
  }

  Widget _buildSkillAnalysis(BuildContext context, AttemptResult result) {
    final writing = result.skillByType('WRITING');
    final speaking = result.skillByType('SPEAKING');

    return Column(
      children: [
        if (writing != null && writing.writingTasks.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: _WritingAnalysisCard(tasks: writing.writingTasks),
          ),
        if (speaking != null && !speaking.feedback.isEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: _FeedbackCard(
              title: 'Speaking - Band ${_formatBand(speaking.bandScore)}',
              feedback: speaking.feedback,
              emptyMessage: 'Chưa có phân tích Speaking.',
            ),
          ),
      ],
    );
  }
}

class _SkillResultCard extends StatelessWidget {
  final String title;
  final String skillType;
  final double? bandScore;

  const _SkillResultCard({
    required this.title,
    required this.skillType,
    required this.bandScore,
  });

  @override
  Widget build(BuildContext context) {
    final color = AppColors.skill(skillType);
    return AccentCard(
      color: color,
      child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(_skillIcon(skillType), color: color),
            const SizedBox(height: 8),
            Text(
              title,
              style: Theme.of(context).textTheme.titleMedium,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            FittedBox(
              fit: BoxFit.scaleDown,
              child: Text(
                _formatSkillBand(skillType, bandScore),
                style: Theme.of(context)
                    .textTheme
                    .headlineSmall
                    ?.copyWith(color: color, fontWeight: FontWeight.w700),
                textAlign: TextAlign.center,
              ),
            ),
          ],
      ),
    );
  }
}

class _WritingAnalysisCard extends StatelessWidget {
  final List<WritingTaskAnalysis> tasks;

  const _WritingAnalysisCard({required this.tasks});

  @override
  Widget build(BuildContext context) {
    return AccentCard(
      color: AppColors.writing,
      child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Writing', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            for (var index = 0; index < tasks.length; index++) ...[
              if (index > 0) const Divider(height: 28),
              Text(
                'Task ${index + 1} - Band ${_formatBand(tasks[index].score)}',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 8),
              _FeedbackSection(feedback: tasks[index].feedback),
            ],
          ],
      ),
    );
  }
}

class _FeedbackCard extends StatelessWidget {
  final String title;
  final AiFeedback feedback;
  final String emptyMessage;

  const _FeedbackCard({
    required this.title,
    required this.feedback,
    required this.emptyMessage,
  });

  @override
  Widget build(BuildContext context) {
    return AccentCard(
      color: _feedbackColor(title),
      child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            feedback.isEmpty
                ? Text(emptyMessage)
                : _FeedbackSection(feedback: feedback),
          ],
      ),
    );
  }
}

class _FeedbackSection extends StatelessWidget {
  final AiFeedback feedback;

  const _FeedbackSection({required this.feedback});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _FeedbackList(
          icon: Icons.check_circle_outline,
          title: 'Điểm mạnh',
          color: AppColors.success,
          items: feedback.strengths,
        ),
        _FeedbackList(
          icon: Icons.warning_amber,
          title: 'Điểm cần cải thiện',
          color: AppColors.warning,
          items: feedback.weaknesses,
        ),
        _FeedbackList(
          icon: Icons.trending_up,
          title: 'Đề xuất cải thiện',
          color: AppColors.primary,
          items: feedback.improvements,
        ),
      ],
    );
  }
}

class _FeedbackList extends StatelessWidget {
  final IconData icon;
  final String title;
  final Color color;
  final List<String> items;

  const _FeedbackList({
    required this.icon,
    required this.title,
    required this.color,
    required this.items,
  });

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const SizedBox.shrink();
    }

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: color, size: 18),
              const SizedBox(width: 8),
              Text(title, style: Theme.of(context).textTheme.titleMedium),
            ],
          ),
          const SizedBox(height: 6),
          ...items.map(
            (item) => Padding(
              padding: const EdgeInsets.only(left: 26, bottom: 4),
              child: Text('• $item'),
            ),
          ),
        ],
      ),
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

Color _feedbackColor(String title) {
  if (title.startsWith('Speaking')) {
    return AppColors.speaking;
  }
  if (title.contains('tổng quan')) {
    return AppColors.primary;
  }
  return AppColors.primary;
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
