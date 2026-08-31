import 'package:flutter/material.dart';

import '../../config/app_colors.dart';
import '../../models/exam.dart';
import '../../services/api_service.dart';
import '../../services/exam_service.dart';
import '../../widgets/state_views.dart';
import '../../widgets/skill_chip.dart';
import 'exam_detail_screen.dart';

class ExamListScreen extends StatefulWidget {
  const ExamListScreen({super.key});

  @override
  State<ExamListScreen> createState() => _ExamListScreenState();
}

class _ExamListScreenState extends State<ExamListScreen> {
  final _examService = ExamService();

  bool _isLoading = true;
  String? _errorMessage;
  List<Exam> _exams = [];

  @override
  void initState() {
    super.initState();
    _loadExams();
  }

  Future<void> _loadExams() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final exams = await _examService.getExams();
      if (!mounted) return;
      setState(() {
        _exams = exams;
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

  void _openExamDetail(Exam exam) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => ExamDetailScreen(examId: exam.id)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Luyện thi')),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const LoadingView(message: 'Đang tải danh sách đề thi...');
    }

    if (_errorMessage != null) {
      return ErrorView(message: _errorMessage!, onRetry: _loadExams);
    }

    if (_exams.isEmpty) {
      return const EmptyState(
        icon: Icons.menu_book,
        message: 'Chưa có đề thi nào.',
      );
    }

    return RefreshIndicator(
      onRefresh: _loadExams,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _exams.length,
        separatorBuilder: (context, index) => const SizedBox(height: 12),
        itemBuilder: (context, index) {
          final exam = _exams[index];
          final accent = exam.premiumOnly ? AppColors.premium : AppColors.primary;
          return Card(
            color: AppColors.soft(accent),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
              side: BorderSide(color: accent.withOpacity(0.28)),
            ),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          exam.title,
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                      ),
                      if (exam.premiumOnly)
                        Chip(
                          avatar: const Icon(Icons.workspace_premium, size: 18),
                          label: const Text('Premium'),
                          backgroundColor: AppColors.soft(AppColors.premium),
                          side: BorderSide(
                            color: AppColors.premium.withOpacity(0.32),
                          ),
                        ),
                    ],
                  ),
                  if (exam.description.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Text(exam.description),
                  ],
                  const SizedBox(height: 12),
                  const Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      SkillChip(skillType: 'LISTENING'),
                      SkillChip(skillType: 'READING'),
                      SkillChip(skillType: 'WRITING'),
                      SkillChip(skillType: 'SPEAKING'),
                    ],
                  ),
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: () => _openExamDetail(exam),
                    icon: const Icon(Icons.chevron_right),
                    label: const Text('Xem đề'),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
