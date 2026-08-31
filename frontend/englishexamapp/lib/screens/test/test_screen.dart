import 'dart:async';

import 'package:flutter/material.dart';
import 'package:just_audio/just_audio.dart';

import '../../config/app_colors.dart';
import '../../config/ielts_time.dart';
import '../../models/attempt.dart';
import '../../models/exam.dart';
import '../../models/result.dart';
import '../../services/api_service.dart';
import '../../services/attempt_service.dart';
import '../../services/mock_session_service.dart';
import '../../services/response_service.dart';
import '../../widgets/accent_card.dart';
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
        _questionIndex =
            _sortedQuestions(_practiceSections[_sectionIndex]).length - 1;
      });
      _syncSkillTimer();
    }
  }

  void _nextQuestion() {
    final section = _practiceSections[_sectionIndex];
    if (_questionIndex < _sortedQuestions(section).length - 1) {
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

  void _jumpToQuestion(_QuestionNavItem item) {
    if (_sessionFinished) {
      return;
    }
    final currentSection = _practiceSections[_sectionIndex];
    final nextSection = _practiceSections[item.sectionIndex];
    _stopAudioIfLeavingListening(currentSection, nextSection);
    setState(() {
      _sectionIndex = item.sectionIndex;
      _questionIndex = item.questionIndex;
    });
    _syncSkillTimer();
  }

  void _openQuestionNavigator() {
    if (_sessionFinished) {
      return;
    }

    final section = _practiceSections[_sectionIndex];
    final navigatorItems = _navigatorItemsForSectionSkill(section);
    if (navigatorItems.isEmpty) {
      return;
    }
    final answeredCount = _answeredCount(navigatorItems);
    final currentNumber = _currentQuestionNumber(navigatorItems);

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (sheetContext) {
        return _QuestionNavigatorSheet(
          items: navigatorItems,
          currentSectionIndex: _sectionIndex,
          currentQuestionIndex: _questionIndex,
          answeredQuestionIds: _answeredQuestionIds,
          answeredCount: answeredCount,
          totalCount: navigatorItems.length,
          currentNumber: currentNumber,
          accentColor: AppColors.skill(section.skillType),
          onSelected: (item) {
            Navigator.pop(sheetContext);
            _jumpToQuestion(item);
          },
        );
      },
    );
  }

  Future<void> _openWriting() async {
    if (_sessionFinished) {
      return;
    }
    _skillTimer?.cancel();
    _sessionPollingTimer?.cancel();
    await _stopListeningAudio();
    if (!mounted) return;
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) =>
            WritingScreen(exam: widget.exam, attempt: widget.attempt),
      ),
    );
    if (mounted && !_sessionFinished) {
      _syncSkillTimer();
      _startSessionPolling();
    }
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
    final skillColor = AppColors.skill(section.skillType);
    final questions = _sortedQuestions(section);
    final question = questions[_questionIndex];
    final navigatorItems = _navigatorItemsForSectionSkill(section);
    final totalQuestions = navigatorItems.length;
    final answeredCount = _answeredCount(navigatorItems);
    final currentNumber = _currentQuestionNumber(navigatorItems);
    final progress = totalQuestions == 0 ? 0.0 : currentNumber / totalQuestions;
    final isSavingCurrentQuestion = _savingQuestionIds.contains(question.id);
    final isFirstPracticeQuestion = _sectionIndex == 0 && _questionIndex == 0;
    final isLastPracticeQuestion =
        _sectionIndex == _practiceSections.length - 1 &&
        _questionIndex == questions.length - 1;

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
            style: Theme.of(context)
                .textTheme
                .headlineSmall
                ?.copyWith(color: skillColor, fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 8),
          LinearProgressIndicator(value: progress, color: skillColor),
          const SizedBox(height: 8),
          if (navigatorItems.isEmpty)
            Text('Câu ${_questionIndex + 1}')
          else
            _QuestionNavigatorBar(
              currentNumber: currentNumber,
              answeredCount: answeredCount,
              totalCount: totalQuestions,
              accentColor: skillColor,
              onPressed: _openQuestionNavigator,
            ),
          const SizedBox(height: 4),
          Text('Thời gian còn lại: ${_formatDuration(_remainingTime)}'),
          if (section.skillType == 'LISTENING' &&
              section.mediaUrl.isNotEmpty) ...[
            const SizedBox(height: 12),
            _buildListeningAudioPlayer(section),
          ],
          if (section.passageContent.isNotEmpty) ...[
            const SizedBox(height: 12),
            AccentCard(
              color: AppColors.skill(section.skillType),
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
          ],
          const SizedBox(height: 12),
          AccentCard(
            color: skillColor,
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
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: _sessionFinished ||
                          isFirstPracticeQuestion ||
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
                    isLastPracticeQuestion ? 'Tiếp tục' : 'Sau',
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

    return AccentCard(
      color: AppColors.listening,
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

  List<Question> _sortedQuestions(ExamSection section) {
    return [...section.questions]
      ..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));
  }

  List<_QuestionNavItem> _navigatorItemsForSectionSkill(
    ExamSection currentSection,
  ) {
    final skillItems = _navigatorItemsForSkill(currentSection.skillType);
    if (skillItems.isNotEmpty) {
      return skillItems;
    }
    return _navigatorItemsForSection(currentSection);
  }

  List<_QuestionNavItem> _navigatorItemsForSkill(String skillType) {
    final items = <_QuestionNavItem>[];
    var number = 1;
    for (
      var sectionIndex = 0;
      sectionIndex < _practiceSections.length;
      sectionIndex++
    ) {
      final section = _practiceSections[sectionIndex];
      if (section.skillType != skillType) {
        continue;
      }
      final questions = _sortedQuestions(section);
      for (
        var questionIndex = 0;
        questionIndex < questions.length;
        questionIndex++
      ) {
        items.add(
          _QuestionNavItem(
            number: number,
            sectionIndex: sectionIndex,
            questionIndex: questionIndex,
            question: questions[questionIndex],
          ),
        );
        number++;
      }
    }
    return items;
  }

  List<_QuestionNavItem> _navigatorItemsForSection(ExamSection section) {
    final sectionIndex = _practiceSections.indexWhere(
      (item) => item.id == section.id,
    );
    if (sectionIndex == -1) {
      return [];
    }
    final questions = _sortedQuestions(section);
    return [
      for (
        var questionIndex = 0;
        questionIndex < questions.length;
        questionIndex++
      )
        _QuestionNavItem(
          number: questionIndex + 1,
          sectionIndex: sectionIndex,
          questionIndex: questionIndex,
          question: questions[questionIndex],
        ),
    ];
  }

  int _answeredCount(List<_QuestionNavItem> items) {
    return items.where((item) {
      return _selectedAnswers[item.question.id]?.isNotEmpty ?? false;
    }).length;
  }

  Set<int> get _answeredQuestionIds {
    return _selectedAnswers.entries
        .where((entry) => entry.value.isNotEmpty)
        .map((entry) => entry.key)
        .toSet();
  }

  int _currentQuestionNumber(List<_QuestionNavItem> items) {
    if (items.isEmpty) {
      return _questionIndex + 1;
    }
    return items
        .firstWhere(
          (item) =>
              item.sectionIndex == _sectionIndex &&
              item.questionIndex == _questionIndex,
          orElse: () => items.first,
        )
        .number;
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

class _QuestionNavItem {
  final int number;
  final int sectionIndex;
  final int questionIndex;
  final Question question;

  const _QuestionNavItem({
    required this.number,
    required this.sectionIndex,
    required this.questionIndex,
    required this.question,
  });
}

class _QuestionNavigatorBar extends StatelessWidget {
  final int currentNumber;
  final int answeredCount;
  final int totalCount;
  final Color accentColor;
  final VoidCallback onPressed;

  const _QuestionNavigatorBar({
    required this.currentNumber,
    required this.answeredCount,
    required this.totalCount,
    required this.accentColor,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return AccentCard(
      color: accentColor,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  'Câu $currentNumber / $totalCount',
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                ),
              ),
              Text('$answeredCount/$totalCount đã làm'),
            ],
          ),
          const SizedBox(height: 8),
          OutlinedButton.icon(
            onPressed: onPressed,
            icon: const Icon(Icons.grid_view),
            label: const Text('Xem câu hỏi'),
          ),
        ],
      ),
    );
  }
}

class _QuestionNavigatorSheet extends StatelessWidget {
  final List<_QuestionNavItem> items;
  final int currentSectionIndex;
  final int currentQuestionIndex;
  final Set<int> answeredQuestionIds;
  final int answeredCount;
  final int totalCount;
  final int currentNumber;
  final Color accentColor;
  final ValueChanged<_QuestionNavItem> onSelected;

  const _QuestionNavigatorSheet({
    required this.items,
    required this.currentSectionIndex,
    required this.currentQuestionIndex,
    required this.answeredQuestionIds,
    required this.answeredCount,
    required this.totalCount,
    required this.currentNumber,
    required this.accentColor,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    final bottomPadding = MediaQuery.of(context).viewInsets.bottom;
    return SafeArea(
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * 0.82,
        ),
        child: SingleChildScrollView(
          padding: EdgeInsets.fromLTRB(16, 4, 16, bottomPadding + 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    'Câu hỏi',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const Spacer(),
                  Text('$answeredCount/$totalCount đã làm'),
                ],
              ),
              const SizedBox(height: 4),
              Text('Đang xem câu $currentNumber'),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: items.map((item) {
                  final isCurrent =
                      item.sectionIndex == currentSectionIndex &&
                      item.questionIndex == currentQuestionIndex;
                  final isAnswered =
                      answeredQuestionIds.contains(item.question.id);
                  return _QuestionNavButton(
                    number: item.number,
                    isCurrent: isCurrent,
                    isAnswered: isAnswered,
                    accentColor: accentColor,
                    onTap: () => onSelected(item),
                  );
                }).toList(),
              ),
              const SizedBox(height: 16),
              Wrap(
                spacing: 16,
                runSpacing: 8,
                children: [
                  _QuestionNavLegend(
                    color: AppColors.soft(AppColors.success),
                    borderColor: AppColors.success,
                    label: 'Đã làm',
                  ),
                  _QuestionNavLegend(
                    color: Theme.of(context).colorScheme.surface,
                    borderColor: Theme.of(context).colorScheme.outlineVariant,
                    label: 'Chưa làm',
                  ),
                  _QuestionNavLegend(
                    color: AppColors.soft(accentColor),
                    borderColor: accentColor,
                    label: 'Đang xem',
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _QuestionNavLegend extends StatelessWidget {
  final Color color;
  final Color borderColor;
  final String label;

  const _QuestionNavLegend({
    required this.color,
    required this.borderColor,
    required this.label,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 14,
          height: 14,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(4),
            border: Border.all(color: borderColor),
          ),
        ),
        const SizedBox(width: 6),
        Text(label),
      ],
    );
  }
}

class _QuestionNavButton extends StatelessWidget {
  final int number;
  final bool isCurrent;
  final bool isAnswered;
  final Color accentColor;
  final VoidCallback onTap;

  const _QuestionNavButton({
    required this.number,
    required this.isCurrent,
    required this.isAnswered,
    required this.accentColor,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final borderColor = isCurrent
        ? accentColor
        : isAnswered
            ? AppColors.success
            : Theme.of(context).colorScheme.outlineVariant;
    final backgroundColor = isCurrent
        ? AppColors.soft(accentColor)
        : isAnswered
            ? AppColors.soft(AppColors.success)
            : Theme.of(context).colorScheme.surface;
    final textColor = isCurrent
        ? accentColor
        : Theme.of(context).colorScheme.onSurface;

    return Material(
      color: backgroundColor,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
        side: BorderSide(
          color: borderColor,
          width: isCurrent ? 2 : 1,
        ),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: onTap,
        child: SizedBox(
          width: 44,
          height: 40,
          child: Center(
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (isAnswered && !isCurrent) ...[
                  Icon(Icons.check, size: 14, color: AppColors.success),
                  const SizedBox(width: 2),
                ],
                Text(
                  '$number',
                  style: TextStyle(
                    color: textColor,
                    fontWeight: isCurrent ? FontWeight.w700 : FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
