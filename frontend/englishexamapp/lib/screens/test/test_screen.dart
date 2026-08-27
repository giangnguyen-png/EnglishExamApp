import 'dart:async';

import 'package:flutter/material.dart';
import 'package:just_audio/just_audio.dart';

import '../../config/ielts_time.dart';
import '../../models/attempt.dart';
import '../../models/exam.dart';
import '../../models/result.dart';
import '../../services/api_service.dart';
import '../../services/attempt_service.dart';
import '../../services/mock_session_service.dart';
import '../../services/response_service.dart';
import '../result/result_screen.dart';
import 'writing_screen.dart';

class TestScreen extends StatefulWidget {
  final Exam exam;
  final Attempt attempt;
  final int listeningMaxPlays;

  const TestScreen({
    super.key,
    required this.exam,
    required this.attempt,
    this.listeningMaxPlays = 2,
  });

  @override
  State<TestScreen> createState() => _TestScreenState();
}

class _TestScreenState extends State<TestScreen> {
  final _responseService = ResponseService();
  final _attemptService = AttemptService();
  final _mockSessionService = MockSessionService();
  final _audioPlayer = AudioPlayer();

  late final List<ExamSection> _practiceSections;
  int _sectionIndex = 0;
  int _questionIndex = 0;
  final Map<int, Set<int>> _selectedAnswers = {};
  final Set<int> _savingQuestionIds = {};
  final Map<int, int> _listeningPlayCounts = {};
  final Map<int, bool> _listeningCompleted = {};
  StreamSubscription<PlayerState>? _playerStateSubscription;
  Timer? _skillTimer;
  Timer? _sessionPollingTimer;
  Duration _remainingTime = Duration.zero;
  String? _activeSkillType;
  bool _sessionFinished = false;

  int? _activeAudioSectionId;
  bool _isAudioLoading = false;
  bool _isAudioPlaying = false;
  bool _isAudioPaused = false;

  @override
  void initState() {
    super.initState();
    _practiceSections =
        widget.exam.sections
            .where(
              (section) =>
                  (section.skillType == 'LISTENING' ||
                      section.skillType == 'READING') &&
                  section.questions.isNotEmpty,
            )
            .toList()
          ..sort((a, b) => a.sectionOrder.compareTo(b.sectionOrder));

    _playerStateSubscription = _audioPlayer.playerStateStream.listen((state) {
      if (!mounted) return;
      if (state.processingState == ProcessingState.completed) {
        setState(() {
          if (_activeAudioSectionId != null) {
            _listeningCompleted[_activeAudioSectionId!] = true;
          }
          _isAudioPlaying = false;
          _isAudioPaused = false;
        });
      } else {
        setState(() {
          _isAudioPlaying = state.playing;
          _isAudioPaused =
              !state.playing && state.processingState == ProcessingState.ready;
        });
      }
    });
    if (_practiceSections.isNotEmpty) {
      _startSkillTimer(_practiceSections.first.skillType);
    }
    _startSessionPolling();
  }

  @override
  void dispose() {
    _skillTimer?.cancel();
    _sessionPollingTimer?.cancel();
    _playerStateSubscription?.cancel();
    _audioPlayer.dispose();
    super.dispose();
  }

  Future<void> _saveAnswer(Question question, Set<int> answerIds) async {
    if (_sessionFinished) {
      return;
    }
    setState(() {
      _savingQuestionIds.add(question.id);
    });

    try {
      await _responseService.saveAnswer(
        widget.attempt.attemptId,
        question.id,
        answerIds.toList(),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
    } finally {
      if (mounted) {
        setState(() {
          _savingQuestionIds.remove(question.id);
        });
      }
    }
  }

  void _selectSingleAnswer(Question question, int answerId) {
    if (_sessionFinished || _savingQuestionIds.contains(question.id)) {
      return;
    }

    final selected = {answerId};
    setState(() {
      _selectedAnswers[question.id] = selected;
    });
    _saveAnswer(question, selected);
  }

  void _toggleMultipleAnswer(Question question, int answerId, bool checked) {
    if (_sessionFinished || _savingQuestionIds.contains(question.id)) {
      return;
    }

    final selected = Set<int>.from(_selectedAnswers[question.id] ?? {});
    if (checked) {
      selected.add(answerId);
    } else {
      selected.remove(answerId);
    }

    setState(() {
      _selectedAnswers[question.id] = selected;
    });
    _saveAnswer(question, selected);
  }

  void _previousQuestion() {
    if (_questionIndex > 0) {
      setState(() {
        _questionIndex--;
      });
      return;
    }

    if (_sectionIndex > 0) {
      final currentSection = _practiceSections[_sectionIndex];
      final nextSection = _practiceSections[_sectionIndex - 1];
      _stopAudioIfLeavingListening(currentSection, nextSection);
      setState(() {
        _sectionIndex--;
        _questionIndex = _practiceSections[_sectionIndex].questions.length - 1;
      });
      _syncSkillTimer();
    }
  }

  void _nextQuestion() {
    final section = _practiceSections[_sectionIndex];
    if (_questionIndex < section.questions.length - 1) {
      setState(() {
        _questionIndex++;
      });
      return;
    }

    if (_sectionIndex < _practiceSections.length - 1) {
      final currentSection = _practiceSections[_sectionIndex];
      final nextSection = _practiceSections[_sectionIndex + 1];
      _stopAudioIfLeavingListening(currentSection, nextSection);
      setState(() {
        _sectionIndex++;
        _questionIndex = 0;
      });
      _syncSkillTimer();
      return;
    }

    _openWriting();
  }

  Future<void> _openWriting() async {
    if (_sessionFinished) {
      return;
    }
    _skillTimer?.cancel();
    await _stopListeningAudio();
    if (!mounted) return;
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) =>
            WritingScreen(exam: widget.exam, attempt: widget.attempt),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_practiceSections.isEmpty) {
      return Scaffold(
        appBar: AppBar(title: Text(widget.exam.title)),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text('Đề thi chưa có câu hỏi Listening hoặc Reading.'),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: _sessionFinished ? null : _openWriting,
                  child: const Text('Tiếp tục Writing'),
                ),
              ],
            ),
          ),
        ),
      );
    }

    final section = _practiceSections[_sectionIndex];
    final questions = section.questions
      ..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));
    final question = questions[_questionIndex];
    final totalQuestions = _practiceSections.fold<int>(
      0,
      (total, item) => total + item.questions.length,
    );
    final doneBefore = _practiceSections
        .take(_sectionIndex)
        .fold<int>(0, (total, item) => total + item.questions.length);
    final currentNumber = doneBefore + _questionIndex + 1;
    final progress = totalQuestions == 0 ? 0.0 : currentNumber / totalQuestions;
    final isSavingCurrentQuestion = _savingQuestionIds.contains(question.id);

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.exam.title),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Center(child: Text(_formatDuration(_remainingTime))),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            widget.exam.title,
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 6),
          Text(
            _skillLabel(section.skillType),
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 8),
          LinearProgressIndicator(value: progress),
          const SizedBox(height: 8),
          Text('Câu $currentNumber/$totalQuestions'),
          const SizedBox(height: 4),
          Text('Thời gian còn lại: ${_formatDuration(_remainingTime)}'),
          if (section.skillType == 'LISTENING' &&
              section.mediaUrl.isNotEmpty) ...[
            const SizedBox(height: 12),
            _buildListeningAudioPlayer(section),
          ],
          if (section.passageContent.isNotEmpty) ...[
            const SizedBox(height: 12),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      section.skillType == 'READING'
                          ? 'Reading Passage'
                          : 'Nội dung bài thi',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 8),
                    Text(section.passageContent),
                  ],
                ),
              ),
            ),
          ],
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Question ${question.orderIndex}',
                    style: Theme.of(context).textTheme.labelLarge,
                  ),
                  const SizedBox(height: 6),
                  Text(
                    question.content,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  _buildQuestionImage(question),
                  const SizedBox(height: 12),
                  ..._buildAnswerOptions(question),
                  if (_savingQuestionIds.contains(question.id)) ...[
                    const SizedBox(height: 8),
                    const LinearProgressIndicator(),
                    const SizedBox(height: 4),
                    const Text('Đang lưu câu trả lời...'),
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
                  onPressed: _sessionFinished ||
                          currentNumber == 1 ||
                          isSavingCurrentQuestion
                      ? null
                      : _previousQuestion,
                  child: const Text('Trước'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton(
                  onPressed: _sessionFinished || isSavingCurrentQuestion
                      ? null
                      : _nextQuestion,
                  child: Text(
                    currentNumber == totalQuestions ? 'Tiếp tục' : 'Sau',
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildListeningAudioPlayer(ExamSection section) {
    final playCount = _listeningPlayCounts[section.id] ?? 0;
    final isCompleted = _listeningCompleted[section.id] ?? false;
    final isActiveSection = _activeAudioSectionId == section.id;
    final isPlaying = isActiveSection && _isAudioPlaying;
    final isPaused = isActiveSection && _isAudioPaused && !isCompleted;
    final hasNoPlayLeft =
        playCount >= widget.listeningMaxPlays && !isPlaying && !isPaused;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.headphones),
                const SizedBox(width: 8),
                Text(
                  'Bài nghe',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text('Lượt nghe: $playCount / ${widget.listeningMaxPlays}'),
            const SizedBox(height: 4),
            Text(_listeningHint(playCount, isPlaying, isPaused)),
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: _sessionFinished || _isAudioLoading || hasNoPlayLeft
                  ? null
                  : () => _handleListeningAudioButton(section),
              icon: _isAudioLoading && isActiveSection
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Icon(_audioButtonIcon(isPlaying, isPaused, hasNoPlayLeft)),
              label: Text(
                _audioButtonText(
                  isPlaying,
                  isPaused,
                  isCompleted,
                  hasNoPlayLeft,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _handleListeningAudioButton(ExamSection section) async {
    if (_activeAudioSectionId == section.id && _isAudioPlaying) {
      await _pauseListeningAudio();
      return;
    }

    if (_activeAudioSectionId == section.id && _isAudioPaused) {
      await _resumeListeningAudio();
      return;
    }

    await _startListeningAudioFromBeginning(section);
  }

  Future<void> _startListeningAudioFromBeginning(ExamSection section) async {
    final playCount = _listeningPlayCounts[section.id] ?? 0;
    if (playCount >= widget.listeningMaxPlays) {
      return;
    }

    final isSameSection = _activeAudioSectionId == section.id;

    setState(() {
      _activeAudioSectionId = section.id;
      _isAudioLoading = true;
    });

    try {
      if (!isSameSection) {
        await _audioPlayer.stop();
        await _audioPlayer.setUrl(section.mediaUrl);
      } else {
        await _audioPlayer.seek(Duration.zero);
      }

      unawaited(_audioPlayer.play());

      if (!mounted) return;
      setState(() {
        _listeningPlayCounts[section.id] = playCount + 1;
        _listeningCompleted[section.id] = false;
        _isAudioPlaying = true;
        _isAudioPaused = false;
      });
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Không thể phát audio: $error')));
    } finally {
      if (mounted) {
        setState(() {
          _isAudioLoading = false;
        });
      }
    }
  }

  Future<void> _pauseListeningAudio() async {
    await _audioPlayer.pause();
    if (!mounted) return;
    setState(() {
      _isAudioPlaying = false;
      _isAudioPaused = true;
    });
  }

  Future<void> _resumeListeningAudio() async {
    unawaited(_audioPlayer.play());
    if (!mounted) return;
    setState(() {
      _isAudioPlaying = true;
      _isAudioPaused = false;
    });
  }

  Future<void> _stopListeningAudio() async {
    await _audioPlayer.stop();
    if (!mounted) return;
    setState(() {
      _isAudioPlaying = false;
      _isAudioPaused = false;
      _isAudioLoading = false;
    });
  }

  void _stopAudioIfLeavingListening(
    ExamSection currentSection,
    ExamSection nextSection,
  ) {
    final leavesCurrentListening =
        currentSection.skillType == 'LISTENING' &&
        currentSection.id != nextSection.id;
    if (leavesCurrentListening) {
      unawaited(_stopListeningAudio());
    }
  }

  String _listeningHint(int playCount, bool isPlaying, bool isPaused) {
    if (playCount == 0) {
      return 'Bạn có tối đa ${widget.listeningMaxPlays} lượt nghe.';
    }
    if (playCount < widget.listeningMaxPlays) {
      return 'Bạn còn ${widget.listeningMaxPlays - playCount} lượt nghe.';
    }
    if (!isPlaying && !isPaused) {
      return 'Bạn đã sử dụng hết lượt nghe.';
    }
    return 'Đây là lượt nghe cuối cùng.';
  }

  IconData _audioButtonIcon(bool isPlaying, bool isPaused, bool hasNoPlayLeft) {
    if (hasNoPlayLeft) {
      return Icons.block;
    }
    if (isPlaying) {
      return Icons.pause;
    }
    return Icons.play_arrow;
  }

  String _audioButtonText(
    bool isPlaying,
    bool isPaused,
    bool isCompleted,
    bool hasNoPlayLeft,
  ) {
    if (hasNoPlayLeft) {
      return 'Đã hết lượt nghe';
    }
    if (isPlaying) {
      return 'Tạm dừng';
    }
    if (isPaused) {
      return 'Tiếp tục';
    }
    if (isCompleted) {
      return 'Nghe lại';
    }
    return 'Bắt đầu nghe';
  }

  List<Widget> _buildAnswerOptions(Question question) {
    final selected = _selectedAnswers[question.id] ?? {};
    final isMultiple = question.questionType == 'MULTIPLE_CHOICE';
    final isSaving = _savingQuestionIds.contains(question.id) || _sessionFinished;

    if (question.answers.isEmpty) {
      return [const Text('Câu hỏi này chưa có đáp án lựa chọn.')];
    }

    if (isMultiple) {
      return question.answers.map((answer) {
        return CheckboxListTile(
          contentPadding: EdgeInsets.zero,
          value: selected.contains(answer.id),
          title: Text(answer.content),
          onChanged: isSaving
              ? null
              : (value) =>
                    _toggleMultipleAnswer(question, answer.id, value ?? false),
        );
      }).toList();
    }

    return [
      RadioGroup<int>(
        groupValue: selected.isEmpty ? null : selected.first,
        onChanged: (value) {
          if (isSaving) {
            return;
          }
          if (value != null) {
            _selectSingleAnswer(question, value);
          }
        },
        child: Column(
          children: question.answers.map((answer) {
            return RadioListTile<int>(
              contentPadding: EdgeInsets.zero,
              enabled: !isSaving,
              value: answer.id,
              title: Text(answer.content),
            );
          }).toList(),
        ),
      ),
    ];
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

  void _startSkillTimer(String skillType) {
    _skillTimer?.cancel();
    _activeSkillType = skillType;
    _remainingTime = IeltsTime.forSkill(skillType);
    if (_remainingTime == Duration.zero) {
      return;
    }
    _skillTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) return;
      if (_remainingTime.inSeconds <= 1) {
        setState(() {
          _remainingTime = Duration.zero;
        });
        _handleSkillTimeExpired();
        return;
      }
      setState(() {
        _remainingTime -= const Duration(seconds: 1);
      });
    });
  }

  void _syncSkillTimer() {
    final skillType = _practiceSections[_sectionIndex].skillType;
    if (skillType != _activeSkillType) {
      _startSkillTimer(skillType);
    }
  }

  void _handleSkillTimeExpired() {
    _skillTimer?.cancel();
    final currentSkill = _activeSkillType;
    if (currentSkill == 'LISTENING') {
      final nextReadingIndex = _practiceSections.indexWhere(
        (section) => section.skillType == 'READING',
      );
      if (nextReadingIndex != -1) {
        _stopListeningAudio();
        setState(() {
          _sectionIndex = nextReadingIndex;
          _questionIndex = 0;
        });
        _startSkillTimer('READING');
        return;
      }
    }
    _openWriting();
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
      // Keep the local test running if a transient polling request fails.
    }
  }

  Future<void> _handleSessionCompleted() async {
    if (_sessionFinished) {
      return;
    }
    setState(() {
      _sessionFinished = true;
    });
    _sessionPollingTimer?.cancel();
    _skillTimer?.cancel();
    await _stopListeningAudio();
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

  String _skillLabel(String skillType) {
    switch (skillType) {
      case 'LISTENING':
        return 'Listening';
      case 'READING':
        return 'Reading';
      default:
        return skillType;
    }
  }
}
