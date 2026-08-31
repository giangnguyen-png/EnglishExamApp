import 'package:flutter/material.dart';

import '../../config/app_colors.dart';
import '../../models/attempt_review.dart';
import '../../services/api_service.dart';
import '../../services/attempt_service.dart';
import '../../widgets/accent_card.dart';
import '../../widgets/state_views.dart';

class AttemptReviewScreen extends StatefulWidget {
  final int attemptId;

  const AttemptReviewScreen({super.key, required this.attemptId});

  @override
  State<AttemptReviewScreen> createState() => _AttemptReviewScreenState();
}

class _AttemptReviewScreenState extends State<AttemptReviewScreen> {
  final _attemptService = AttemptService();

  bool _isLoading = true;
  String? _errorMessage;
  AttemptReview? _review;
  String? _selectedSkill;

  @override
  void initState() {
    super.initState();
    _loadReview();
  }

  Future<void> _loadReview() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final review = await _attemptService.getAttemptReview(widget.attemptId);
      if (!mounted) return;
      setState(() {
        _review = review;
        _selectedSkill = review.sections.isEmpty
            ? null
            : review.sections.first.skillType;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _errorMessage = ApiService.getErrorMessage(error);
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Xem lại bài làm')),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const LoadingView(message: 'Đang tải bài làm...');
    }
    if (_errorMessage != null) {
      return ErrorView(message: _errorMessage!, onRetry: _loadReview);
    }

    final review = _review;
    if (review == null || review.sections.isEmpty) {
      return const EmptyState(
        icon: Icons.rate_review_outlined,
        message: 'Không có dữ liệu xem lại cho bài này.',
      );
    }

    final skills = review.sections.map((section) => section.skillType).toSet();
    final selectedSkill = _selectedSkill ?? skills.first;
    final sections = review.sections
        .where((section) => section.skillType == selectedSkill)
        .toList();

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          review.examTitle,
          style: Theme.of(context).textTheme.headlineSmall,
        ),
        const SizedBox(height: 12),
        if (skills.length > 1)
          _SkillSelector(
            skills: skills.toList(),
            selectedSkill: selectedSkill,
            onChanged: (skill) {
              setState(() {
                _selectedSkill = skill;
              });
            },
          ),
        const SizedBox(height: 12),
        for (final section in sections)
          _ReviewSectionCard(section: section),
      ],
    );
  }
}

class _SkillSelector extends StatelessWidget {
  final List<String> skills;
  final String selectedSkill;
  final ValueChanged<String> onChanged;

  const _SkillSelector({
    required this.skills,
    required this.selectedSkill,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: skills.map((skill) {
        final selected = skill == selectedSkill;
        final color = AppColors.skill(skill);
        return ChoiceChip(
          selected: selected,
          avatar: Icon(_skillIcon(skill), size: 18, color: color),
          label: Text(_skillLabel(skill)),
          onSelected: (_) => onChanged(skill),
          selectedColor: AppColors.soft(color),
          side: BorderSide(color: color.withOpacity(selected ? 0.6 : 0.24)),
        );
      }).toList(),
    );
  }
}

class _ReviewSectionCard extends StatelessWidget {
  final ReviewSection section;

  const _ReviewSectionCard({required this.section});

  @override
  Widget build(BuildContext context) {
    final color = AppColors.skill(section.skillType);
    return AccentCard(
      color: color,
      margin: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(_skillIcon(section.skillType), color: color),
              const SizedBox(width: 8),
              Text(
                '${_skillLabel(section.skillType)} ${section.sectionOrder}',
                style: Theme.of(context).textTheme.titleLarge,
              ),
            ],
          ),
          if (section.skillType == 'READING' &&
              section.passageContent.trim().isNotEmpty) ...[
            const SizedBox(height: 12),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: color.withOpacity(0.18)),
              ),
              child: Text(section.passageContent),
            ),
          ],
          const SizedBox(height: 12),
          for (final question in section.questions)
            _QuestionReviewCard(question: question),
        ],
      ),
    );
  }
}

class _QuestionReviewCard extends StatelessWidget {
  final ReviewQuestion question;

  const _QuestionReviewCard({required this.question});

  @override
  Widget build(BuildContext context) {
    final statusColor = question.correct ? AppColors.success : AppColors.error;
    final statusText = question.answered
        ? question.correct
            ? 'Đúng'
            : 'Sai'
        : 'Chưa trả lời';

    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: statusColor.withOpacity(0.28)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(
                  'Câu ${question.orderIndex}: ${question.content}',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              const SizedBox(width: 8),
              Chip(
                label: Text(statusText),
                avatar: Icon(
                  question.correct
                      ? Icons.check_circle_outline
                      : Icons.cancel_outlined,
                  size: 18,
                ),
                backgroundColor: AppColors.soft(statusColor),
                side: BorderSide(color: statusColor.withOpacity(0.35)),
              ),
            ],
          ),
          if (question.imageUrl != null) ...[
            const SizedBox(height: 12),
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: Image.network(question.imageUrl!),
            ),
          ],
          const SizedBox(height: 10),
          for (final answer in question.answers)
            _AnswerReviewTile(answer: answer),
        ],
      ),
    );
  }
}

class _AnswerReviewTile extends StatelessWidget {
  final ReviewAnswer answer;

  const _AnswerReviewTile({required this.answer});

  @override
  Widget build(BuildContext context) {
    final color = answer.correct
        ? AppColors.success
        : answer.selected
            ? AppColors.error
            : Colors.grey;
    final labels = <Widget>[
      if (answer.selected)
        _AnswerBadge(text: 'Bạn đã chọn', color: AppColors.primary),
      if (answer.correct)
        _AnswerBadge(text: 'Đáp án đúng', color: AppColors.success),
    ];

    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: AppColors.soft(color),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color.withOpacity(0.28)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(
                answer.correct
                    ? Icons.check_circle_outline
                    : answer.selected
                        ? Icons.radio_button_checked
                        : Icons.radio_button_unchecked,
                size: 20,
                color: color,
              ),
              const SizedBox(width: 8),
              Expanded(child: Text(answer.content)),
            ],
          ),
          if (labels.isNotEmpty) ...[
            const SizedBox(height: 8),
            Wrap(spacing: 6, runSpacing: 6, children: labels),
          ],
          if (answer.explanation.trim().isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
              answer.explanation,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
        ],
      ),
    );
  }
}

class _AnswerBadge extends StatelessWidget {
  final String text;
  final Color color;

  const _AnswerBadge({required this.text, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        text,
        style: Theme.of(context)
            .textTheme
            .labelSmall
            ?.copyWith(color: color, fontWeight: FontWeight.w700),
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
    default:
      return Icons.rate_review;
  }
}

String _skillLabel(String skillType) {
  switch (skillType) {
    case 'LISTENING':
      return 'Listening';
    case 'READING':
      return 'Reading';
    default:
      return skillType;
  }
}
