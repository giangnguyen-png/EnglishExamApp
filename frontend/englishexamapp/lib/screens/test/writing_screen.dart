import 'package:flutter/material.dart';

import '../../models/attempt.dart';
import '../../models/exam.dart';
import '../../services/api_service.dart';
import '../../services/response_service.dart';
import 'speaking_screen.dart';

class WritingScreen extends StatefulWidget {
  final Exam exam;
  final Attempt attempt;

  const WritingScreen({super.key, required this.exam, required this.attempt});

  @override
  State<WritingScreen> createState() => _WritingScreenState();
}

class _WritingScreenState extends State<WritingScreen> {
  final _responseService = ResponseService();
  final Map<int, TextEditingController> _controllers = {};

  late final List<Question> _writingQuestions;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _writingQuestions =
        widget.exam.sections
            .where((section) => section.skillType == 'WRITING')
            .expand((section) => section.questions)
            .toList()
          ..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));

    for (final question in _writingQuestions) {
      _controllers[question.id] = TextEditingController();
    }
  }

  @override
  void dispose() {
    for (final controller in _controllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  Future<void> _saveWriting() async {
    for (final question in _writingQuestions) {
      final text = _controllers[question.id]!.text.trim();
      if (text.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Vui lòng nhập bài Writing Task ${_taskNumber(question)}.',
            ),
          ),
        );
        return;
      }
    }

    setState(() {
      _isSaving = true;
    });

    try {
      for (final question in _writingQuestions) {
        await _responseService.submitWriting(
          widget.attempt.attemptId,
          question.id,
          _controllers[question.id]!.text.trim(),
        );
      }

      if (!mounted) return;
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) =>
              SpeakingScreen(exam: widget.exam, attempt: widget.attempt),
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
          _isSaving = false;
        });
      }
    }
  }

  void _skipToSpeaking() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) =>
            SpeakingScreen(exam: widget.exam, attempt: widget.attempt),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Writing')),
      body: _writingQuestions.isEmpty
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Text('Đề thi chưa có câu hỏi Writing.'),
                    const SizedBox(height: 16),
                    FilledButton(
                      onPressed: _skipToSpeaking,
                      child: const Text('Tiếp tục Speaking'),
                    ),
                  ],
                ),
              ),
            )
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Text(
                  'Writing',
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
                const SizedBox(height: 12),
                ..._writingQuestions.map(_buildWritingTask),
                const SizedBox(height: 12),
                FilledButton.icon(
                  onPressed: _isSaving ? null : _saveWriting,
                  icon: _isSaving
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.save),
                  label: const Text('Lưu bài Writing'),
                ),
              ],
            ),
    );
  }

  Widget _buildWritingTask(Question question) {
    final controller = _controllers[question.id]!;

    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Writing Task ${_taskNumber(question)}',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 8),
            Text(question.content),
            const SizedBox(height: 12),
            TextField(
              controller: controller,
              maxLines: null,
              minLines: 8,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                hintText: 'Nhập bài viết của bạn...',
              ),
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 8),
            Text('Số từ: ${_wordCount(controller.text)}'),
          ],
        ),
      ),
    );
  }

  int _taskNumber(Question question) {
    final index = _writingQuestions.indexWhere(
      (item) => item.id == question.id,
    );
    return index + 1;
  }

  int _wordCount(String text) {
    final trimmed = text.trim();
    if (trimmed.isEmpty) {
      return 0;
    }
    return trimmed.split(RegExp(r'\s+')).length;
  }
}
