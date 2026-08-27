import 'dart:async';

import 'package:flutter/material.dart';

import '../../config/ielts_time.dart';
import '../../models/attempt.dart';
import '../../models/exam.dart';
import '../../models/result.dart';
import '../../services/api_service.dart';
import '../../services/attempt_service.dart';
import '../../services/mock_session_service.dart';
import '../../services/response_service.dart';
import '../result/result_screen.dart';
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
  final _attemptService = AttemptService();
  final _mockSessionService = MockSessionService();
  final Map<int, TextEditingController> _controllers = {};
  final Map<int, Timer> _autosaveTimers = {};
  final Set<int> _draftSavingQuestionIds = {};

  late final List<Question> _writingQuestions;
  Timer? _timer;
  Timer? _sessionPollingTimer;
  Duration _remainingTime = IeltsTime.writing;
  bool _isSaving = false;
  bool _sessionFinished = false;

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
    _startSessionPolling();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _sessionPollingTimer?.cancel();
    for (final timer in _autosaveTimers.values) {
      timer.cancel();
    }
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
      await _saveAllWritingDrafts();
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
    if (_sessionFinished) {
      return;
    }
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
                      onPressed: _sessionFinished ? null : _skipToSpeaking,
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
                  onPressed: _isSaving || _sessionFinished
                      ? null
                      : () => _saveWriting(),
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
              enabled: !_sessionFinished,
              maxLines: null,
              minLines: 8,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                hintText: 'Nhập bài viết của bạn...',
              ),
              onChanged: (_) {
                _scheduleWritingAutosave(question);
                setState(() {});
              },
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

  void _scheduleWritingAutosave(Question question) {
    if (_sessionFinished) {
      return;
    }
    _autosaveTimers[question.id]?.cancel();
    _autosaveTimers[question.id] = Timer(const Duration(seconds: 3), () {
      unawaited(_saveWritingDraft(question));
    });
  }

  Future<void> _saveWritingDraft(Question question) async {
    if (!mounted || _sessionFinished || _draftSavingQuestionIds.contains(question.id)) {
      return;
    }
    setState(() {
      _draftSavingQuestionIds.add(question.id);
    });
    try {
      await _responseService.saveWritingDraft(
        widget.attempt.attemptId,
        question.id,
        _controllers[question.id]!.text,
      );
    } catch (_) {
      // Draft failures are surfaced when the user explicitly saves or submits.
    } finally {
      if (mounted) {
        setState(() {
          _draftSavingQuestionIds.remove(question.id);
        });
      }
    }
  }

  Future<void> _saveAllWritingDrafts() async {
    for (final timer in _autosaveTimers.values) {
      timer.cancel();
    }
    _autosaveTimers.clear();
    for (final question in _writingQuestions) {
      await _responseService.saveWritingDraft(
        widget.attempt.attemptId,
        question.id,
        _controllers[question.id]!.text,
      );
    }
  }

  void _startSessionPolling() {
    final sessionId = widget.attempt.sessionId;
    if (sessionId == null) {
      return;
    }
    _sessionPollingTimer?.cancel();
    _sessionPollingTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      unawaited(_checkSessionStatus(sessionId));
    });
  }

  Future<void> _checkSessionStatus(int sessionId) async {
    if (_sessionFinished) {
      return;
    }
    try {
      final session = await _mockSessionService.getSession(sessionId);
      if (session.status == 'COMPLETED') {
        await _handleSessionCompleted();
      }
    } catch (_) {
      // Keep editing if a transient polling request fails.
    }
  }

  Future<void> _handleSessionCompleted() async {
    if (_sessionFinished) {
      return;
    }
    setState(() {
      _sessionFinished = true;
      _isSaving = true;
    });
    _sessionPollingTimer?.cancel();
    _timer?.cancel();
    await _saveAllWritingDrafts();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text(
          'Ca thi đã kết thúc. Hệ thống đang chấm phần bài bạn đã hoàn thành.',
        ),
      ),
    );
    final AttemptResult result = await _attemptService.forceSubmitAttempt(
      widget.attempt.attemptId,
    );
    if (!mounted) return;
    Navigator.pushAndRemoveUntil(
      context,
      MaterialPageRoute(builder: (_) => ResultScreen(result: result)),
      (route) => route.isFirst,
    );
  }

  String _formatDuration(Duration duration) {
    final totalSeconds = duration.inSeconds.clamp(0, 24 * 60 * 60);
    final minutes = (totalSeconds ~/ 60).toString().padLeft(2, '0');
    final seconds = (totalSeconds % 60).toString().padLeft(2, '0');
    return '$minutes:$seconds';
  }
}
