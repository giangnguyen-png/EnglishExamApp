import 'package:flutter/material.dart';

import '../../models/exam.dart';
import '../../services/api_service.dart';
import '../../services/attempt_service.dart';
import '../../services/exam_service.dart';
import '../../widgets/state_views.dart';
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
      if (!mounted) return;
      setState(() {
        _exam = exam;
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
          const Chip(label: Text('Premium')),
        ],
        const SizedBox(height: 20),
        if (exam.premiumOnly)
          const Card(
            child: Padding(
              padding: EdgeInsets.all(16),
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
            ),
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
        const SizedBox(height: 20),
        if (exam.sections.isEmpty)
          const Text('Đề thi chưa có section.')
        else
          ...exam.sections.map(_buildSection),
      ],
    );
  }

  Widget _buildSection(ExamSection section) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              section.skillType,
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 4),
            Text('${section.questionCount} câu hỏi'),
          ],
        ),
      ),
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
    } finally {
      if (mounted) {
        setState(() {
          _isStartingAttempt = false;
        });
      }
    }
  }
}
