import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:just_audio/just_audio.dart';

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
  final _audioPlayer = AudioPlayer();
  final Map<String, int> _selectedQuestionBySkill = {};

  bool _isLoading = true;
  bool _isPlayingAudio = false;
  String? _errorMessage;
  AttemptReview? _review;
  String? _selectedSkill;

  @override
  void initState() {
    super.initState();
    _loadReview();
  }

  @override
  void dispose() {
    _audioPlayer.dispose();
    super.dispose();
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
        _selectedSkill = review.skillTypes.isEmpty
            ? null
            : review.skillTypes.first;
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

    final selectedSkill = _selectedSkill ?? review.skillTypes.first;
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
        _SkillSelector(
          skills: review.skillTypes,
          selectedSkill: selectedSkill,
          onChanged: (skill) {
            setState(() {
              _selectedSkill = skill;
            });
          },
        ),
        const SizedBox(height: 12),
        if (selectedSkill == 'LISTENING' || selectedSkill == 'READING')
          _buildObjectiveReview(selectedSkill, sections)
        else if (selectedSkill == 'WRITING')
          _WritingReview(sections: sections)
        else if (selectedSkill == 'SPEAKING')
          _SpeakingReview(
            sections: sections,
            onPlayAudio: _playAudio,
            isPlayingAudio: _isPlayingAudio,
          ),
      ],
    );
  }

  Widget _buildObjectiveReview(String skillType, List<ReviewSection> sections) {
    final questions = _objectiveQuestions(sections);
    if (questions.isEmpty) {
      return const EmptyState(
        icon: Icons.quiz_outlined,
        message: 'Không có câu hỏi để xem lại.',
      );
    }

    final selectedIndex = (_selectedQuestionBySkill[skillType] ?? 0)
        .clamp(0, questions.length - 1)
        .toInt();
    final current = questions[selectedIndex];
    final correctCount = questions.where((item) => item.question.correct).length;
    final wrongCount = questions
        .where((item) => item.question.answered && !item.question.correct)
        .length;
    final unansweredCount =
        questions.where((item) => !item.question.answered).length;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        AccentCard(
          color: AppColors.skill(skillType),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      'Câu ${selectedIndex + 1}/${questions.length}',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  Text(
                    '$correctCount đúng • $wrongCount sai • $unansweredCount chưa làm',
                    textAlign: TextAlign.right,
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                ],
              ),
              const SizedBox(height: 12),
              OutlinedButton.icon(
                onPressed: () => _showQuestionNavigator(
                  skillType,
                  questions,
                  selectedIndex,
                ),
                icon: const Icon(Icons.grid_view),
                label: const Text('Xem câu hỏi'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        _ObjectiveQuestionCard(
          section: current.section,
          question: current.question,
          relativeNumber: selectedIndex + 1,
        ),
      ],
    );
  }

  List<_ObjectiveQuestionRef> _objectiveQuestions(List<ReviewSection> sections) {
    final result = <_ObjectiveQuestionRef>[];
    for (final section in sections) {
      for (final question in section.questions) {
        result.add(_ObjectiveQuestionRef(section: section, question: question));
      }
    }
    return result;
  }

  Future<void> _showQuestionNavigator(
    String skillType,
    List<_ObjectiveQuestionRef> questions,
    int selectedIndex,
  ) async {
    await showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
            child: GridView.builder(
              shrinkWrap: true,
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 8,
                mainAxisSpacing: 8,
                crossAxisSpacing: 8,
              ),
              itemCount: questions.length,
              itemBuilder: (context, index) {
                final question = questions[index].question;
                final color = _questionStatusColor(question);
                final selected = index == selectedIndex;
                return InkWell(
                  borderRadius: BorderRadius.circular(8),
                  onTap: () {
                    Navigator.pop(context);
                    setState(() {
                      _selectedQuestionBySkill[skillType] = index;
                    });
                  },
                  child: Container(
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: AppColors.soft(color),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(
                        color: selected ? AppColors.primary : color,
                        width: selected ? 2 : 1,
                      ),
                    ),
                    child: Text(
                      '${index + 1}',
                      style: TextStyle(
                        color: color,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        );
      },
    );
  }

  Future<void> _playAudio(String audioUrl) async {
    try {
      setState(() {
        _isPlayingAudio = true;
      });
      await _audioPlayer.stop();
      await _audioPlayer.setUrl(audioUrl);
      await _audioPlayer.play();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể phát audio.')),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isPlayingAudio = false;
        });
      }
    }
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

class _ObjectiveQuestionCard extends StatelessWidget {
  final ReviewSection section;
  final ReviewQuestion question;
  final int relativeNumber;

  const _ObjectiveQuestionCard({
    required this.section,
    required this.question,
    required this.relativeNumber,
  });

  @override
  Widget build(BuildContext context) {
    final color = _questionStatusColor(question);
    return AccentCard(
      color: color,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (section.skillType == 'READING' &&
              section.passageContent.trim().isNotEmpty) ...[
            Text('Passage', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            SelectableText(section.passageContent),
            const Divider(height: 28),
          ],
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(
                  'Câu $relativeNumber: ${question.content}',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              const SizedBox(width: 8),
              _StatusChip(question: question),
            ],
          ),
          if (question.imageUrl != null) ...[
            const SizedBox(height: 12),
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: Image.network(question.imageUrl!),
            ),
          ],
          const SizedBox(height: 12),
          for (final answer in question.answers)
            _AnswerReviewTile(answer: answer),
        ],
      ),
    );
  }
}

class _WritingReview extends StatelessWidget {
  final List<ReviewSection> sections;

  const _WritingReview({required this.sections});

  @override
  Widget build(BuildContext context) {
    final bandScore = _firstBandScore(sections);
    final aiAnalysis = _firstAiAnalysis(sections);
    final feedback = _FeedbackContent.fromAnalysis(aiAnalysis);
    final questions = sections.expand((section) => section.questions).toList();

    return AccentCard(
      color: AppColors.writing,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Writing', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          Text('Band: ${_formatBand(bandScore)}'),
          const SizedBox(height: 12),
          for (var index = 0; index < questions.length; index++) ...[
            if (index > 0) const Divider(height: 28),
            Text(
              'Task ${index + 1}',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text('Đề bài:', style: Theme.of(context).textTheme.labelLarge),
            const SizedBox(height: 4),
            SelectableText(questions[index].content),
            const SizedBox(height: 10),
            Text(
              'Bài làm của bạn:',
              style: Theme.of(context).textTheme.labelLarge,
            ),
            const SizedBox(height: 4),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppColors.writing.withOpacity(0.18)),
              ),
              child: SelectableText(
                questions[index].textResponse.trim().isEmpty
                    ? 'Chưa có bài làm.'
                    : questions[index].textResponse,
              ),
            ),
            const SizedBox(height: 8),
            Text('Điểm AI: ${_formatBand(questions[index].aiScore)}'),
          ],
          if (!feedback.isEmpty) ...[
            const Divider(height: 28),
            Text(
              'Đánh giá Writing',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            _FeedbackSection(feedback: feedback),
          ],
        ],
      ),
    );
  }
}

class _SpeakingReview extends StatelessWidget {
  final List<ReviewSection> sections;
  final ValueChanged<String> onPlayAudio;
  final bool isPlayingAudio;

  const _SpeakingReview({
    required this.sections,
    required this.onPlayAudio,
    required this.isPlayingAudio,
  });

  @override
  Widget build(BuildContext context) {
    final bandScore = _firstBandScore(sections);
    final aiAnalysis = _firstAiAnalysis(sections);
    final feedback = _FeedbackContent.fromAnalysis(aiAnalysis);
    final questions = sections.expand((section) => section.questions).toList();

    return AccentCard(
      color: AppColors.speaking,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Speaking', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          Text(
            bandScore == null
                ? 'Chưa có điểm từ giám khảo.'
                : 'Band: ${_formatBand(bandScore)}',
          ),
          const SizedBox(height: 12),
          for (var index = 0; index < questions.length; index++) ...[
            if (index > 0) const Divider(height: 28),
            Text(
              'Câu ${index + 1}',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text('Question:', style: Theme.of(context).textTheme.labelLarge),
            const SizedBox(height: 4),
            SelectableText(questions[index].content),
            const SizedBox(height: 10),
            Text('Transcript:', style: Theme.of(context).textTheme.labelLarge),
            const SizedBox(height: 4),
            SelectableText(
              questions[index].transcript.trim().isEmpty
                  ? 'Không có transcript.'
                  : questions[index].transcript,
            ),
            if (questions[index].audioUrl != null) ...[
              const SizedBox(height: 10),
              OutlinedButton.icon(
                onPressed: isPlayingAudio
                    ? null
                    : () => onPlayAudio(questions[index].audioUrl!),
                icon: const Icon(Icons.play_arrow),
                label: const Text('Nghe lại câu trả lời'),
              ),
            ],
            if (questions[index].aiScore != null) ...[
              const SizedBox(height: 8),
              Text('Điểm câu: ${_formatBand(questions[index].aiScore)}'),
            ],
          ],
          if (!feedback.isEmpty) ...[
            const Divider(height: 28),
            Text(
              'Đánh giá Speaking',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            _FeedbackSection(feedback: feedback),
          ],
        ],
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  final ReviewQuestion question;

  const _StatusChip({required this.question});

  @override
  Widget build(BuildContext context) {
    final color = _questionStatusColor(question);
    return Chip(
      label: Text(_questionStatusText(question)),
      avatar: Icon(_questionStatusIcon(question), size: 18),
      backgroundColor: AppColors.soft(color),
      side: BorderSide(color: color.withOpacity(0.35)),
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
                        ? Icons.cancel_outlined
                        : Icons.radio_button_unchecked,
                size: 20,
                color: color,
              ),
              const SizedBox(width: 8),
              Expanded(child: SelectableText(answer.content)),
            ],
          ),
          if (labels.isNotEmpty) ...[
            const SizedBox(height: 8),
            Wrap(spacing: 6, runSpacing: 6, children: labels),
          ],
          if (answer.explanation.trim().isNotEmpty) ...[
            const SizedBox(height: 8),
            SelectableText(answer.explanation),
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

class _FeedbackSection extends StatelessWidget {
  final _FeedbackContent feedback;

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
          title: 'Đề xuất',
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

class _FeedbackContent {
  final List<String> strengths;
  final List<String> weaknesses;
  final List<String> improvements;

  const _FeedbackContent({
    this.strengths = const [],
    this.weaknesses = const [],
    this.improvements = const [],
  });

  bool get isEmpty =>
      strengths.isEmpty && weaknesses.isEmpty && improvements.isEmpty;

  factory _FeedbackContent.fromAnalysis(String value) {
    if (value.trim().isEmpty) {
      return const _FeedbackContent();
    }

    try {
      final decoded = jsonDecode(value);
      if (decoded is Map<String, dynamic>) {
        final feedback = decoded['feedback'];
        if (feedback is Map<String, dynamic>) {
          return _FeedbackContent.fromMap(feedback);
        }
        return _FeedbackContent.fromMap(decoded);
      }
      if (decoded is List<dynamic>) {
        return _FeedbackContent.fromEntries(decoded);
      }
    } catch (_) {
      return const _FeedbackContent();
    }

    return const _FeedbackContent();
  }

  factory _FeedbackContent.fromEntries(List<dynamic> entries) {
    final strengths = <String>[];
    final weaknesses = <String>[];
    final improvements = <String>[];
    for (final entry in entries.whereType<Map<String, dynamic>>()) {
      final feedback = entry['feedback'];
      if (feedback is Map<String, dynamic>) {
        strengths.addAll(_readStringList(feedback['strengths']));
        weaknesses.addAll(_readStringList(feedback['weaknesses']));
        improvements.addAll(_readStringList(feedback['improvements']));
      }
    }
    return _FeedbackContent(
      strengths: strengths,
      weaknesses: weaknesses,
      improvements: improvements,
    );
  }

  factory _FeedbackContent.fromMap(Map<String, dynamic> json) {
    return _FeedbackContent(
      strengths: _readStringList(json['strengths']),
      weaknesses: _readStringList(json['weaknesses']),
      improvements: [
        ..._readStringList(json['improvements']),
        ..._readStringList(json['suggestions']),
      ],
    );
  }
}

class _ObjectiveQuestionRef {
  final ReviewSection section;
  final ReviewQuestion question;

  const _ObjectiveQuestionRef({
    required this.section,
    required this.question,
  });
}

Color _questionStatusColor(ReviewQuestion question) {
  if (!question.answered) {
    return Colors.grey;
  }
  return question.correct ? AppColors.success : AppColors.error;
}

IconData _questionStatusIcon(ReviewQuestion question) {
  if (!question.answered) {
    return Icons.remove_circle_outline;
  }
  return question.correct ? Icons.check_circle_outline : Icons.cancel_outlined;
}

String _questionStatusText(ReviewQuestion question) {
  if (!question.answered) {
    return '— Chưa trả lời';
  }
  return question.correct ? '✓ Đúng' : '✕ Sai';
}

double? _firstBandScore(List<ReviewSection> sections) {
  for (final section in sections) {
    if (section.bandScore != null) {
      return section.bandScore;
    }
  }
  return null;
}

String _firstAiAnalysis(List<ReviewSection> sections) {
  for (final section in sections) {
    if (section.aiAnalysis.trim().isNotEmpty) {
      return section.aiAnalysis;
    }
  }
  return '';
}

List<String> _readStringList(dynamic value) {
  if (value is! List<dynamic>) {
    return const [];
  }
  return value
      .map((item) => item?.toString().trim() ?? '')
      .where((item) => item.isNotEmpty)
      .toList();
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
      return Icons.rate_review;
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

String _formatBand(double? value) {
  if (value == null) {
    return 'Chưa có điểm';
  }
  return value.toStringAsFixed(1);
}
