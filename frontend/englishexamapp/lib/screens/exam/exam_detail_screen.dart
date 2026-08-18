import 'package:flutter/material.dart';

import '../../models/exam.dart';
import '../../services/api_service.dart';
import '../../services/attempt_service.dart';
import '../../services/exam_service.dart';
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
      appBar: AppBar(title: const Text('Exam Detail')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
          ? _buildError()
          : exam == null
          ? const Center(child: Text('Khong tim thay de thi.'))
          : _buildExamDetail(exam),
    );
  }

  Widget _buildError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              _errorMessage!,
              textAlign: TextAlign.center,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
            const SizedBox(height: 16),
            OutlinedButton(
              onPressed: _loadExamDetail,
              child: const Text('Thu lai'),
            ),
          ],
        ),
      ),
    );
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
          Text(
            'Premium only',
            style: TextStyle(
              color: Theme.of(context).colorScheme.primary,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
        const SizedBox(height: 20),
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
          const Text('De thi chua co section.')
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
            Text('Thu tu section: ${section.sectionOrder}'),
            if (section.mediaUrl.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text('Media: ${section.mediaUrl}'),
            ],
            if (section.passageContent.isNotEmpty) ...[
              const SizedBox(height: 12),
              Text(section.passageContent),
            ],
            const SizedBox(height: 12),
            if (section.questions.isEmpty)
              const Text('Section nay chua co cau hoi.')
            else
              ...section.questions.map(_buildQuestion),
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
      if (!mounted) return;
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => TestScreen(exam: exam, attempt: attempt),
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

  Widget _buildQuestion(Question question) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${question.orderIndex}. ${question.content}',
            style: const TextStyle(fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 4),
          Text('Loai cau hoi: ${question.questionType}'),
          if (question.answers.isNotEmpty) ...[
            const SizedBox(height: 6),
            ...question.answers.map(
              (answer) => Padding(
                padding: const EdgeInsets.only(left: 12, bottom: 4),
                child: Text('- ${answer.content}'),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
