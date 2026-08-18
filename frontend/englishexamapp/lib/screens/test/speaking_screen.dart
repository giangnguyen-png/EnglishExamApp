import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';

import '../../models/attempt.dart';
import '../../models/exam.dart';
import '../../models/result.dart';
import '../../services/api_service.dart';
import '../../services/attempt_service.dart';
import '../../services/response_service.dart';
import '../result/result_screen.dart';

class SpeakingScreen extends StatefulWidget {
  final Exam exam;
  final Attempt attempt;

  const SpeakingScreen({super.key, required this.exam, required this.attempt});

  @override
  State<SpeakingScreen> createState() => _SpeakingScreenState();
}

class _SpeakingScreenState extends State<SpeakingScreen> {
  final _audioRecorder = AudioRecorder();
  final _responseService = ResponseService();
  final _attemptService = AttemptService();

  late final List<Question> _speakingQuestions;
  final Map<int, String> _audioPaths = {};
  final Set<int> _uploadedQuestionIds = {};

  int _questionIndex = 0;
  int? _recordingQuestionId;
  int? _uploadingQuestionId;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _speakingQuestions =
        widget.exam.sections
            .where((section) => section.skillType == 'SPEAKING')
            .expand((section) => section.questions)
            .toList()
          ..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));
  }

  @override
  void dispose() {
    _audioRecorder.dispose();
    super.dispose();
  }

  Future<void> _startRecording(Question question) async {
    try {
      final hasPermission = await _audioRecorder.hasPermission();
      if (!hasPermission) {
        if (!mounted) return;
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
      });
    } catch (error) {
      if (!mounted) return;
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

  Future<void> _uploadSpeaking(Question question) async {
    final path = _audioPaths[question.id];
    if (path == null || path.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng ghi âm trước khi gửi câu trả lời.'),
        ),
      );
      return;
    }

    setState(() {
      _uploadingQuestionId = question.id;
    });

    try {
      await _responseService.submitSpeaking(
        widget.attempt.attemptId,
        question.id,
        path,
      );

      if (!mounted) return;
      setState(() {
        _uploadedQuestionIds.add(question.id);
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã gửi câu trả lời Speaking.')),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
    } finally {
      if (mounted) {
        setState(() {
          _uploadingQuestionId = null;
        });
      }
    }
  }

  Future<void> _confirmSubmit() async {
    if (_recordingQuestionId != null || _uploadingQuestionId != null) {
      return;
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
    setState(() {
      _isSubmitting = true;
    });

    try {
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
    if (_questionIndex > 0) {
      setState(() {
        _questionIndex--;
      });
    }
  }

  void _nextQuestion() {
    if (_questionIndex < _speakingQuestions.length - 1) {
      setState(() {
        _questionIndex++;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isSubmitting) {
      return const Scaffold(
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              CircularProgressIndicator(),
              SizedBox(height: 16),
              Text('Đang chấm bài...'),
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
              onPressed: _isSubmitting || isBusy ? null : _confirmSubmit,
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
    final isBusy = _recordingQuestionId != null || _uploadingQuestionId != null;
    final hasAudio = _audioPaths.containsKey(question.id);
    final isUploaded = _uploadedQuestionIds.contains(question.id);

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Speaking', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 8),
        LinearProgressIndicator(
          value: (_questionIndex + 1) / _speakingQuestions.length,
        ),
        const SizedBox(height: 8),
        Text('Câu ${_questionIndex + 1}/${_speakingQuestions.length}'),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Question:',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 8),
                Text(question.content),
                const SizedBox(height: 16),
                if (isRecording) ...[
                  const LinearProgressIndicator(),
                  const SizedBox(height: 8),
                  const Text('Đang ghi âm...'),
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: () => _stopRecording(question),
                    icon: const Icon(Icons.stop),
                    label: const Text('Dừng'),
                  ),
                ] else ...[
                  FilledButton.icon(
                    onPressed: !isBusy ? () => _startRecording(question) : null,
                    icon: const Icon(Icons.mic),
                    label: Text(hasAudio ? 'Ghi âm lại' : 'Bắt đầu ghi âm'),
                  ),
                ],
                if (hasAudio && !isRecording) ...[
                  const SizedBox(height: 12),
                  Text(isUploaded ? 'Đã gửi câu trả lời' : 'Đã có file ghi âm'),
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: isUploading
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
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _questionIndex == 0 || isBusy
                    ? null
                    : _previousQuestion,
                child: const Text('Trước'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: OutlinedButton(
                onPressed:
                    _questionIndex == _speakingQuestions.length - 1 || isBusy
                    ? null
                    : _nextQuestion,
                child: const Text('Sau'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        FilledButton(
          onPressed: _isSubmitting || isBusy ? null : _confirmSubmit,
          child: const Text('Nộp bài'),
        ),
      ],
    );
  }
}
