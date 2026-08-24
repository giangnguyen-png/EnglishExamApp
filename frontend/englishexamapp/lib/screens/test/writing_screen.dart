import 'dart:async';

import 'package:flutter/material.dart';

import '../../config/ielts_time.dart';
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
  Timer? _timer;
  Duration _remainingTime = IeltsTime.writing;
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
    _startTimer();
  }

  @override
  void dispose() {
    _timer?.cancel();
    for (final controller in _controllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  Future<void> _saveWriting({bool requireAllAnswers = true}) async {
    if (requireAllAnswers) {
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
    }

    setState(() {
      _isSaving = true;
    });

    try {
      for (final question in _writingQuestions) {
        final text = _controllers[question.id]!.text.trim();
        if (text.isNotEmpty) {
          await _responseService.submitWriting(
            widget.attempt.attemptId,
            question.id,
            text,
          );
        }
      }

      if (!mounted) return;
      _timer?.cancel();
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
    _timer?.cancel();
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
      appBar: AppBar(
        title: const Text('Writing'),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Center(child: Text(_formatDuration(_remainingTime))),
          ),
        ],
      ),
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
                const SizedBox(height: 6),
                Text('Thời gian còn lại: ${_formatDuration(_remainingTime)}'),
                const SizedBox(height: 12),
                ..._writingQuestions.map(_buildWritingTask),
                const SizedBox(height: 12),
                FilledButton.icon(
                  onPressed: _isSaving ? null : () => _saveWriting(),
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
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Text(question.content),
              ),
            ),
            _buildQuestionImage(question),
            const SizedBox(height: 12),
            Text(
              'Bài viết của bạn',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
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
            Align(
              alignment: Alignment.centerRight,
              child: Chip(label: Text('Số từ: ${_wordCount(controller.text)}')),
            ),
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

  Widget _buildQuestionImage(Question question) {
    final imageUrl = question.imageUrl;
    if (imageUrl == null || imageUrl.isEmpty) {
      return const SizedBox.shrink();
    }

    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: Image.network(
          imageUrl,
          fit: BoxFit.contain,
          loadingBuilder: (context, child, loadingProgress) {
            if (loadingProgress == null) {
              return child;
            }
            return const SizedBox(
              height: 180,
              child: Center(child: CircularProgressIndicator()),
            );
          },
          errorBuilder: (context, error, stackTrace) {
            return const SizedBox.shrink();
          },
        ),
      ),
    );
  }

  void _startTimer() {
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) return;
      if (_remainingTime.inSeconds <= 1) {
        setState(() {
          _remainingTime = Duration.zero;
        });
        _timer?.cancel();
        _saveWriting(requireAllAnswers: false);
        return;
      }
      setState(() {
        _remainingTime -= const Duration(seconds: 1);
      });
    });
  }

  String _formatDuration(Duration duration) {
    final totalSeconds = duration.inSeconds.clamp(0, 24 * 60 * 60);
    final minutes = (totalSeconds ~/ 60).toString().padLeft(2, '0');
    final seconds = (totalSeconds % 60).toString().padLeft(2, '0');
    return '$minutes:$seconds';
  }
}
