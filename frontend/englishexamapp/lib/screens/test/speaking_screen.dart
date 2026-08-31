import 'dart:async';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';

import '../../config/app_colors.dart';
import '../../models/attempt.dart';
import '../../models/exam.dart';
import '../../models/result.dart';
import '../../services/api_service.dart';
import '../../services/attempt_service.dart';
import '../../services/mock_session_service.dart';
import '../../services/response_service.dart';
import '../../widgets/accent_card.dart';
import '../result/result_screen.dart';

class SpeakingScreen extends StatefulWidget {
  final Exam exam;
  final Attempt attempt;

  const SpeakingScreen({super.key, required this.exam, required this.attempt});

  @override
  State<SpeakingScreen> createState() => _SpeakingScreenState();
}

class _SpeakingScreenState extends State<SpeakingScreen> {
  static const int defaultReadingSeconds = 3;

  final _audioRecorder = AudioRecorder();
  final _responseService = ResponseService();
  final _attemptService = AttemptService();
  final _mockSessionService = MockSessionService();

  late final List<Question> _speakingQuestions;
  final Map<int, String> _audioPaths = {};
  final Set<int> _uploadedQuestionIds = {};

  int _questionIndex = 0;
  int? _recordingQuestionId;
  int? _uploadingQuestionId;
  Timer? _questionTimer;
  Timer? _sessionPollingTimer;
  int _remainingSeconds = 0;
  bool _isPreparing = false;
  bool _isReadingQuestion = false;
  bool _isFinishingTimedQuestion = false;
  String? _uploadError;
  bool _isSubmitting = false;
  bool _sessionFinished = false;

  @override
  void initState() {
    super.initState();
    _speakingQuestions =
        widget.exam.sections
            .where((section) => section.skillType == 'SPEAKING')
            .expand((section) => section.questions)
            .toList()
          ..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted && _speakingQuestions.isNotEmpty) {
        _startQuestionFlow(_speakingQuestions.first);
      }
    });
    _startSessionPolling();
  }

  @override
  void dispose() {
    _questionTimer?.cancel();
    _sessionPollingTimer?.cancel();
    if (_recordingQuestionId != null) {
      unawaited(_audioRecorder.stop());
    }
    _audioRecorder.dispose();
    super.dispose();
  }

  Future<void> _startRecording(Question question) async {
    if (_sessionFinished ||
        _isSubmitting ||
        _uploadingQuestionId != null ||
        _recordingQuestionId != null) {
      return;
    }

    try {
      final hasPermission = await _audioRecorder.hasPermission();
      if (!hasPermission) {
        if (!mounted) return;
        setState(() {
          _isPreparing = false;
          _isReadingQuestion = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Bạn cần cấp quyền microphone để ghi âm.'),
          ),
        );
        return;
      }

      final directory = await getTemporaryDirectory();
      final path =
          '${directory.path}/speaking_${widget.attempt.attemptId}_${question.id}.wav';

      await _audioRecorder.start(
        const RecordConfig(
          encoder: AudioEncoder.wav,
          sampleRate: 16000,
          numChannels: 1,
        ),
        path: path,
      );

      if (!mounted) return;
      setState(() {
        _recordingQuestionId = question.id;
        _isPreparing = false;
        _isReadingQuestion = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _isPreparing = false;
        _isReadingQuestion = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Không thể bắt đầu ghi âm: ${ApiService.getErrorMessage(error)}',
          ),
        ),
      );
    }
  }

  Future<void> _stopRecording(Question question) async {
    if (_recordingQuestionId != question.id) {
      return;
    }

    try {
      final path = await _audioRecorder.stop();
      if (!mounted) return;
      setState(() {
        _recordingQuestionId = null;
        if (path != null && path.isNotEmpty) {
          _audioPaths[question.id] = path;
          _uploadedQuestionIds.remove(question.id);
        }
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _recordingQuestionId = null;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Không thể dừng ghi âm: ${ApiService.getErrorMessage(error)}',
          ),
        ),
      );
    }
  }

  Future<bool> _uploadSpeaking(
    Question question, {
    bool autoAdvance = false,
    bool allowWhileSubmitting = false,
  }) async {
    if (_sessionFinished ||
        (!allowWhileSubmitting && _isSubmitting) ||
        _uploadingQuestionId != null) {
      return false;
    }

    final path = _audioPaths[question.id];
    if (path == null || path.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng ghi âm trước khi gửi câu trả lời.'),
        ),
      );
      return false;
    }

    setState(() {
      _uploadingQuestionId = question.id;
      _uploadError = null;
    });

    try {
      await _responseService.submitSpeaking(
        widget.attempt.attemptId,
        question.id,
        path,
      );

      if (!mounted) return false;
      setState(() {
        _uploadedQuestionIds.add(question.id);
      });
      if (!autoAdvance) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã gửi câu trả lời Speaking.')),
        );
      }
      if (autoAdvance) {
        _goToNextQuestionAfterUpload();
      }
      return true;
    } catch (error) {
      if (!mounted) return false;
      final message = ApiService.getErrorMessage(error);
      setState(() {
        _uploadError = message;
      });
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(message)));
      return false;
    } finally {
      if (mounted) {
        setState(() {
          _uploadingQuestionId = null;
        });
      }
    }
  }

  Future<void> _confirmSubmit() async {
    if (_sessionFinished ||
        _isSubmitting ||
        _isReadingQuestion ||
        _isPreparing ||
        _uploadingQuestionId != null ||
        _isFinishingTimedQuestion) {
      return;
    }

    if (_speakingQuestions.isNotEmpty) {
      final currentQuestion = _speakingQuestions[_questionIndex];
      final currentQuestionCanBeSubmitted =
          _recordingQuestionId == currentQuestion.id ||
          _audioPaths.containsKey(currentQuestion.id);
      final missingUploadedCount = _speakingQuestions
          .where(
            (question) =>
                !_uploadedQuestionIds.contains(question.id) &&
                (question.id != currentQuestion.id ||
                    !currentQuestionCanBeSubmitted),
          )
          .length;
      if (missingUploadedCount > 0) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Vui lòng gửi câu trả lời cho tất cả câu Speaking trước khi nộp bài.',
            ),
          ),
        );
        return;
      }
    }

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Nộp bài?'),
          content: const Text(
            'Sau khi nộp, bạn không thể chỉnh sửa câu trả lời.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Hủy'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Nộp bài'),
            ),
          ],
        );
      },
    );

    if (confirmed == true) {
      _submitAttempt();
    }
  }

  Future<void> _submitAttempt() async {
    if (_sessionFinished || _isSubmitting) {
      return;
    }
    _questionTimer?.cancel();
    setState(() {
      _isSubmitting = true;
      _isReadingQuestion = false;
      _isPreparing = false;
    });

    try {
      final currentQuestion = _speakingQuestions.isEmpty
          ? null
          : _speakingQuestions[_questionIndex];
      if (currentQuestion != null) {
        if (_recordingQuestionId == currentQuestion.id) {
          await _stopRecording(currentQuestion);
        }
        if (!mounted) return;
        if (!_uploadedQuestionIds.contains(currentQuestion.id) &&
            _audioPaths.containsKey(currentQuestion.id)) {
          final uploaded = await _uploadSpeaking(
            currentQuestion,
            allowWhileSubmitting: true,
          );
          if (!uploaded) {
            if (mounted) {
              setState(() {
                _isSubmitting = false;
              });
            }
            return;
          }
        }
      }
      if (!mounted) return;
      final AttemptResult result = await _attemptService.submitAttempt(
        widget.attempt.attemptId,
      );
      if (!mounted) return;
      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(builder: (_) => ResultScreen(result: result)),
        (route) => route.isFirst,
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  void _previousQuestion() {
    if (_questionIndex > 0 && !_isSubmitting && !_sessionFinished) {
      setState(() {
        _questionIndex--;
      });
      _startQuestionFlow(_speakingQuestions[_questionIndex]);
    }
  }

  void _nextQuestion() {
    if (_questionIndex < _speakingQuestions.length - 1 &&
        !_isSubmitting &&
        !_sessionFinished) {
      setState(() {
        _questionIndex++;
      });
      _startQuestionFlow(_speakingQuestions[_questionIndex]);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isSubmitting || _sessionFinished) {
      return const Scaffold(
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              CircularProgressIndicator(),
              SizedBox(height: 16),
              Text('Đang nộp bài...'),
              SizedBox(height: 8),
              Padding(
                padding: EdgeInsets.symmetric(horizontal: 24),
                child: Text(
                  'Hệ thống đang phân tích kết quả, vui lòng chờ trong giây lát.',
                  textAlign: TextAlign.center,
                ),
              ),
            ],
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Speaking')),
      body: _speakingQuestions.isEmpty
          ? _buildEmptySpeaking()
          : _buildSpeaking(),
    );
  }

  Widget _buildEmptySpeaking() {
    final isBusy = _recordingQuestionId != null || _uploadingQuestionId != null;

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Đề thi chưa có câu hỏi Speaking.'),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _isSubmitting || _sessionFinished || isBusy
                  ? null
                  : _confirmSubmit,
              child: const Text('Nộp bài'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSpeaking() {
    final question = _speakingQuestions[_questionIndex];
    final isRecording = _recordingQuestionId == question.id;
    final isUploading = _uploadingQuestionId == question.id;
    final isBusy =
        _recordingQuestionId != null ||
        _uploadingQuestionId != null ||
        _isFinishingTimedQuestion;
    final hasAudio = _audioPaths.containsKey(question.id);
    final isUploaded = _uploadedQuestionIds.contains(question.id);
    final canLeaveQuestion =
        !isBusy && !_isPreparing && !_isReadingQuestion && isUploaded;
    final answerSeconds = _answerSeconds(question);
    final canSubmit =
        !_isSubmitting &&
        !_sessionFinished &&
        _uploadingQuestionId == null &&
        !_isFinishingTimedQuestion &&
        !_isReadingQuestion &&
        !_isPreparing;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          'Speaking',
          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                color: AppColors.speaking,
                fontWeight: FontWeight.w700,
              ),
        ),
        const SizedBox(height: 8),
        LinearProgressIndicator(
          value: (_questionIndex + 1) / _speakingQuestions.length,
          color: AppColors.speaking,
        ),
        const SizedBox(height: 8),
        Text('Câu ${_questionIndex + 1}/${_speakingQuestions.length}'),
        const SizedBox(height: 12),
        AccentCard(
          color: AppColors.speaking,
          child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Câu hỏi:',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 8),
                Text(question.content),
                _buildQuestionImage(question),
                const SizedBox(height: 16),
                Center(
                  child: Text(
                    _formatSeconds(_remainingSeconds),
                    style: Theme.of(context).textTheme.displaySmall,
                  ),
                ),
                const SizedBox(height: 8),
                Center(
                  child: Text(_speakingStatusText(isRecording, isUploading)),
                ),
                const SizedBox(height: 16),
                Center(
                  child: Icon(
                    isRecording ? Icons.fiber_manual_record : Icons.mic,
                    size: 56,
                    color: isRecording
                        ? Theme.of(context).colorScheme.error
                        : AppColors.speaking,
                  ),
                ),
                const SizedBox(height: 12),
                if (isRecording) ...[
                  const LinearProgressIndicator(),
                  const SizedBox(height: 8),
                  const Text('Đang ghi âm...'),
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: _isSubmitting || _sessionFinished
                        ? null
                        : () {
                            _questionTimer?.cancel();
                            _stopRecording(question);
                          },
                    icon: const Icon(Icons.stop),
                    label: const Text('Dừng'),
                  ),
                ] else if (_isPreparing) ...[
                  FilledButton.icon(
                    onPressed: isBusy || _isSubmitting || _sessionFinished
                        ? null
                        : () => _beginAnswerPhase(question, answerSeconds),
                    icon: const Icon(Icons.play_arrow),
                    label: const Text('Bắt đầu trả lời'),
                  ),
                ] else ...[
                  FilledButton.icon(
                    onPressed: !isBusy &&
                            !_isReadingQuestion &&
                            !_isPreparing &&
                            !_isSubmitting &&
                            !_sessionFinished
                        ? () => _startRecording(question)
                        : null,
                    icon: const Icon(Icons.mic),
                    label: Text(hasAudio ? 'Ghi âm lại' : 'Bắt đầu ghi âm'),
                  ),
                ],
                if (_uploadError != null) ...[
                  const SizedBox(height: 12),
                  Text(
                    'Upload lỗi: $_uploadError',
                    style: TextStyle(
                      color: Theme.of(context).colorScheme.error,
                    ),
                  ),
                ],
                if (hasAudio && !isRecording) ...[
                  const SizedBox(height: 12),
                  Chip(
                    avatar: Icon(isUploaded ? Icons.check : Icons.mic),
                    label: Text(
                      isUploaded ? 'Đã gửi câu trả lời' : 'Đã ghi âm',
                    ),
                  ),
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: isUploading || _isSubmitting || _sessionFinished
                        ? null
                        : () => _uploadSpeaking(question),
                    icon: isUploading
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.upload),
                    label: const Text('Gửi câu trả lời'),
                  ),
                ],
              ],
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed:
                    _questionIndex == 0 ||
                        isBusy ||
                        _isReadingQuestion ||
                        _isPreparing
                    ? null
                    : _previousQuestion,
                child: const Text('Trước'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: OutlinedButton(
                onPressed:
                    _questionIndex == _speakingQuestions.length - 1 ||
                        !canLeaveQuestion
                    ? null
                    : _nextQuestion,
                child: const Text('Sau'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        FilledButton(
          onPressed: canSubmit ? _confirmSubmit : null,
          child: const Text('Nộp bài'),
        ),
      ],
    );
  }

  void _startQuestionFlow(Question question) {
    if (_sessionFinished) {
      return;
    }
    _questionTimer?.cancel();
    _uploadError = null;
    final duration = question.durationSeconds ?? 30;
    final preparation = _validPreparationSeconds(question, duration);
    final isUploaded = _uploadedQuestionIds.contains(question.id);
    setState(() {
      _remainingSeconds = duration;
      _isPreparing = preparation > 0 && !isUploaded;
      _isReadingQuestion = preparation == 0 && !isUploaded;
      _isFinishingTimedQuestion = false;
      if (_isReadingQuestion) {
        _remainingSeconds = defaultReadingSeconds;
      }
    });
    if (isUploaded || _isSubmitting) {
      return;
    }
    if (preparation == 0) {
      _questionTimer = Timer.periodic(const Duration(seconds: 1), (_) {
        _tickReadingQuestion(question, duration);
      });
      return;
    }
    _questionTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      _tickQuestion(question, duration, preparation);
    });
  }

  void _tickQuestion(Question question, int duration, int preparation) {
    if (!mounted ||
        _isSubmitting ||
        _uploadedQuestionIds.contains(question.id)) {
      _questionTimer?.cancel();
      return;
    }
    final answerStartsAt = duration - preparation;
    if (_remainingSeconds <= answerStartsAt + 1) {
      setState(() {
        _isPreparing = false;
        _remainingSeconds = answerStartsAt;
      });
      _questionTimer?.cancel();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Hết thời gian chuẩn bị. Bắt đầu trả lời.'),
        ),
      );
      _beginAnswerPhase(question, answerStartsAt);
      return;
    }

    setState(() {
      _remainingSeconds--;
    });
  }

  void _tickReadingQuestion(Question question, int answerSeconds) {
    if (!mounted ||
        _isSubmitting ||
        _uploadedQuestionIds.contains(question.id)) {
      _questionTimer?.cancel();
      return;
    }
    if (_remainingSeconds <= 1) {
      _questionTimer?.cancel();
      _beginAnswerPhase(question, answerSeconds);
      return;
    }

    setState(() {
      _remainingSeconds--;
    });
  }

  Future<void> _beginAnswerPhase(Question question, int answerSeconds) async {
    if (_sessionFinished ||
        _isSubmitting ||
        _recordingQuestionId != null ||
        _uploadingQuestionId != null ||
        _uploadedQuestionIds.contains(question.id)) {
      return;
    }
    _questionTimer?.cancel();
    if (mounted) {
      setState(() {
        _remainingSeconds = answerSeconds;
        _isPreparing = false;
        _isReadingQuestion = false;
      });
    }
    await _startRecording(question);
    if (!mounted || _recordingQuestionId != question.id || _isSubmitting) {
      return;
    }
    _questionTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      _tickAnswerQuestion(question);
    });
  }

  void _tickAnswerQuestion(Question question) {
    if (!mounted ||
        _isSubmitting ||
        _uploadedQuestionIds.contains(question.id)) {
      _questionTimer?.cancel();
      return;
    }
    if (_remainingSeconds <= 1) {
      setState(() {
        _remainingSeconds = 0;
      });
      _questionTimer?.cancel();
      _finishTimedQuestion(question);
      return;
    }

    setState(() {
      _remainingSeconds--;
    });
  }

  Future<void> _finishTimedQuestion(Question question) async {
    if (_sessionFinished || _isSubmitting || _isFinishingTimedQuestion) {
      return;
    }
    setState(() {
      _isFinishingTimedQuestion = true;
    });
    if (_recordingQuestionId == question.id) {
      await _stopRecording(question);
    }
    if (!mounted || _isSubmitting) return;
    await _uploadSpeaking(question, autoAdvance: true);
    if (!mounted) return;
    setState(() {
      _isFinishingTimedQuestion = false;
    });
  }

  void _goToNextQuestionAfterUpload() {
    if (!mounted || _isSubmitting) return;
    if (_questionIndex < _speakingQuestions.length - 1) {
      setState(() {
        _questionIndex++;
      });
      _startQuestionFlow(_speakingQuestions[_questionIndex]);
    }
  }

  int _validPreparationSeconds(Question question, int duration) {
    final preparation = question.preparationSeconds ?? 0;
    if (preparation <= 0 || preparation >= duration) {
      return 0;
    }
    return preparation;
  }

  int _answerSeconds(Question question) {
    final duration = question.durationSeconds ?? 30;
    final preparation = _validPreparationSeconds(question, duration);
    return preparation > 0 ? duration - preparation : duration;
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

  String _speakingStatusText(bool isRecording, bool isUploading) {
    if (_isSubmitting) {
      return 'Đang nộp bài...';
    }
    if (isUploading) {
      return 'Đang gửi câu trả lời...';
    }
    if (_isReadingQuestion) {
      return 'Đọc câu hỏi';
    }
    if (_isPreparing) {
      return 'Thời gian chuẩn bị';
    }
    if (isRecording) {
      return 'Bắt đầu trả lời - Đang ghi âm';
    }
    return 'Sẵn sàng ghi âm';
  }

  String _formatSeconds(int totalSeconds) {
    final safeSeconds = totalSeconds.clamp(0, 24 * 60 * 60);
    final minutes = (safeSeconds ~/ 60).toString().padLeft(2, '0');
    final seconds = (safeSeconds % 60).toString().padLeft(2, '0');
    return '$minutes:$seconds';
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
      // Keep the speaking flow running if a transient polling request fails.
    }
  }

  Future<void> _handleSessionCompleted() async {
    if (_sessionFinished) {
      return;
    }
    if (_uploadingQuestionId != null) {
      return;
    }
    setState(() {
      _sessionFinished = true;
      _isSubmitting = true;
      _isPreparing = false;
      _isReadingQuestion = false;
    });
    _sessionPollingTimer?.cancel();
    _questionTimer?.cancel();
    if (_recordingQuestionId != null) {
      await _audioRecorder.stop();
      _recordingQuestionId = null;
    }
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
}
