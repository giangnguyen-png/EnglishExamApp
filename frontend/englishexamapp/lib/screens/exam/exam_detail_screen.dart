import 'package:flutter/material.dart';

import '../../config/app_colors.dart';
import '../../models/attempt_review.dart';
import '../../models/exam.dart';
import '../../services/api_service.dart';
import '../../services/attempt_service.dart';
import '../../services/exam_service.dart';
import '../../widgets/state_views.dart';
import '../../widgets/accent_card.dart';
import '../../widgets/skill_chip.dart';
import '../premium/premium_intro_screen.dart';
import '../test/test_screen.dart';

class ExamDetailScreen extends StatefulWidget {
  final int examId;

  const ExamDetailScreen({super.key, required this.examId});

  @override
  State<ExamDetailScreen> createState() => _ExamDetailScreenState();
}

class _ExamDetailScreenState extends State<ExamDetailScreen> {
  final _examService = ExamService();
  final _attemptService = AttemptService();

  bool _isLoading = true;
  bool _isStartingAttempt = false;
  String? _errorMessage;
  Exam? _exam;
  FreeQuota? _quota;

  @override
  void initState() {
    super.initState();
    _loadExamDetail();
  }

  Future<void> _loadExamDetail() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final exam = await _examService.getExamDetail(widget.examId);
      FreeQuota? quota;
      if (!exam.premiumOnly) {
        try {
          quota = await _attemptService.getFreeQuota();
        } catch (_) {
          quota = null;
        }
      }
      if (!mounted) return;
      setState(() {
        _exam = exam;
        _quota = quota;
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
    final exam = _exam;

    return Scaffold(
      appBar: AppBar(title: const Text('Chi tiết đề thi')),
      body: _isLoading
          ? const LoadingView(message: 'Đang tải đề thi...')
          : _errorMessage != null
          ? _buildError()
          : exam == null
          ? const EmptyState(
              icon: Icons.search_off,
              message: 'Không tìm thấy đề thi.',
            )
          : _buildExamDetail(exam),
    );
  }

  Widget _buildError() {
    return ErrorView(message: _errorMessage!, onRetry: _loadExamDetail);
  }

  Widget _buildExamDetail(Exam exam) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(exam.title, style: Theme.of(context).textTheme.headlineSmall),
        if (exam.description.isNotEmpty) ...[
          const SizedBox(height: 8),
          Text(exam.description),
        ],
        if (exam.premiumOnly) ...[
          const SizedBox(height: 8),
          Chip(
            avatar: const Icon(Icons.workspace_premium, size: 18),
            label: const Text('Premium'),
            backgroundColor: AppColors.soft(AppColors.premium),
            side: BorderSide(color: AppColors.premium.withOpacity(0.32)),
          ),
        ],
        const SizedBox(height: 20),
        if (exam.premiumOnly)
          const AccentCard(
            color: AppColors.premium,
            child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.workspace_premium),
                      SizedBox(width: 8),
                      Text('Đề thi Premium'),
                    ],
                  ),
                  SizedBox(height: 8),
                  Text('Hãy đăng ký và tham gia Mock Session để làm đề này.'),
                ],
            ),
          )
        else
          _buildNormalPracticePanel(exam),
        const SizedBox(height: 20),
        if (exam.sections.isEmpty)
          const Text('Đề thi chưa có section.')
        else
          ...exam.sections.map(_buildSection),
      ],
    );
  }

  Widget _buildSection(ExamSection section) {
    return AccentCard(
      color: AppColors.skill(section.skillType),
      margin: const EdgeInsets.only(bottom: 16),
      child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SkillChip(skillType: section.skillType),
            const SizedBox(height: 8),
            Text(
              '${section.questionCount} câu hỏi',
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ],
      ),
    );
  }

  Widget _buildNormalPracticePanel(Exam exam) {
    final quota = _quota;
    final blocked = quota != null && !quota.premium && quota.remaining == 0;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (quota != null) ...[
          AccentCard(
            color: quota.premium ? AppColors.premium : AppColors.primary,
            child: Row(
              children: [
                Icon(
                  quota.premium
                      ? Icons.workspace_premium
                      : Icons.confirmation_number_outlined,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    quota.premium
                        ? 'Premium • Luyện tập không giới hạn'
                        : 'Lượt luyện tập miễn phí: ${quota.used}/${quota.limit}',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
        ],
        if (blocked)
          FilledButton.icon(
            onPressed: _openPremiumIntro,
            icon: const Icon(Icons.workspace_premium),
            label: const Text('Nâng cấp Premium'),
          )
        else
          FilledButton.icon(
            onPressed: _isStartingAttempt ? null : () => _startAttempt(exam),
            icon: _isStartingAttempt
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.play_arrow),
            label: const Text('Bắt đầu luyện tập'),
          ),
      ],
    );
  }

  Future<void> _startAttempt(Exam exam) async {
    setState(() {
      _isStartingAttempt = true;
    });

    try {
      final attempt = await _attemptService.startAttempt(exam.id);
      final fullExam = await _attemptService.getAttemptExam(attempt.attemptId);
      if (!mounted) return;
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => TestScreen(exam: fullExam, attempt: attempt),
        ),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
      _loadExamDetail();
    } finally {
      if (mounted) {
        setState(() {
          _isStartingAttempt = false;
        });
      }
    }
  }

  void _openPremiumIntro() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const PremiumIntroScreen()),
    );
  }
}
