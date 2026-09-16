import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:englishexamapp/models/attempt.dart';
import 'package:englishexamapp/models/exam.dart';
import 'package:englishexamapp/models/mock_session.dart';
import 'package:englishexamapp/models/result.dart';
import 'package:englishexamapp/screens/test/speaking_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:path_provider_platform_interface/path_provider_platform_interface.dart';
import 'package:record_platform_interface/record_platform_interface.dart';

import '../../helpers/test_data.dart';

void main() {
  late RecordPlatform originalRecordPlatform;
  late PathProviderPlatform originalPathProviderPlatform;
  late FakeRecordPlatform recordPlatform;
  late Directory tempDirectory;

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    originalRecordPlatform = RecordPlatform.instance;
    originalPathProviderPlatform = PathProviderPlatform.instance;
    recordPlatform = FakeRecordPlatform();
    tempDirectory = Directory.systemTemp.createTempSync(
      'speaking_screen_test_',
    );
    RecordPlatform.instance = recordPlatform;
    PathProviderPlatform.instance = FakePathProviderPlatform(
      tempDirectory.path,
    );
  });

  tearDown(() {
    RecordPlatform.instance = originalRecordPlatform;
    PathProviderPlatform.instance = originalPathProviderPlatform;
    if (tempDirectory.existsSync()) {
      tempDirectory.deleteSync(recursive: true);
    }
  });

  testWidgets(
    'speakingScreen_whenNoSpeakingQuestions_shouldRenderEmptyStateAndSubmitButton',
    (tester) async {
      final exam = Exam(
        id: 1,
        title: 'IELTS Practice 1',
        description: '',
        premiumOnly: false,
        sections: const [],
      );

      await tester.pumpWidget(
        MaterialApp(
          home: SpeakingScreen(exam: exam, attempt: attempt()),
        ),
      );

      expect(find.text('Đề thi chưa có câu hỏi Speaking.'), findsOneWidget);
      expect(find.text('Nộp bài'), findsOneWidget);
    },
  );

  testWidgets(
    'speakingScreen_whenNonLastTimedQuestionEnds_shouldUploadAndMoveNext',
    (tester) async {
      var uploadCount = 0;
      var submitCount = 0;

      await _pumpSpeakingScreen(
        tester,
        speakingExam(questionCount: 2),
        submitSpeaking: (attemptId, questionId, path) async {
          uploadCount++;
        },
        submitAttempt: (attemptId) async {
          submitCount++;
          return attemptResult();
        },
      );
      await _startTimedAnswer(tester);
      await _finishTimedAnswer(tester);

      expect(recordPlatform.stopCount, 1);
      expect(uploadCount, 1);
      expect(submitCount, 0);
      expect(find.text('Câu 2/2'), findsOneWidget);
    },
  );

  testWidgets(
    'speakingScreen_whenLastTimedQuestionEnds_shouldUploadSubmitOnceAndShowResult',
    (tester) async {
      var uploadCount = 0;
      var submitCount = 0;

      await _pumpSpeakingScreen(
        tester,
        speakingExam(questionCount: 1),
        submitSpeaking: (attemptId, questionId, path) async {
          uploadCount++;
        },
        submitAttempt: (attemptId) async {
          submitCount++;
          return attemptResult();
        },
      );
      await _startTimedAnswer(tester);
      await _finishTimedAnswer(tester);
      await tester.pump();

      expect(recordPlatform.stopCount, 1);
      expect(uploadCount, 1);
      expect(submitCount, 1);
      expect(find.text('Kết quả'), findsOneWidget);
    },
  );

  testWidgets(
    'speakingScreen_whenLastQuestionUploadFails_shouldNotSubmitAndCanRetry',
    (tester) async {
      var uploadCount = 0;
      var submitCount = 0;

      await _pumpSpeakingScreen(
        tester,
        speakingExam(questionCount: 1),
        submitSpeaking: (attemptId, questionId, path) async {
          uploadCount++;
          final requestOptions = RequestOptions(path: '/speaking');
          throw DioException(
            requestOptions: requestOptions,
            response: Response<Map<String, dynamic>>(
              requestOptions: requestOptions,
              statusCode: 500,
              data: {'message': 'Upload failed'},
            ),
          );
        },
        submitAttempt: (attemptId) async {
          submitCount++;
          return attemptResult();
        },
      );
      await _startTimedAnswer(tester);
      await _finishTimedAnswer(tester);
      await tester.pump();

      expect(uploadCount, 1);
      expect(submitCount, 0);
      expect(find.text('Kết quả'), findsNothing);
      expect(find.text('Upload lỗi: Upload failed'), findsOneWidget);

      final retryButton = tester.widget<FilledButton>(
        find.widgetWithText(FilledButton, 'Gửi câu trả lời'),
      );
      expect(retryButton.onPressed, isNotNull);
    },
  );

  testWidgets(
    'speakingScreen_whenSubmittingAndSessionCompletes_shouldNotForceSubmit',
    (tester) async {
      final submitCompleter = Completer<AttemptResult>();
      var submitCount = 0;
      var forceSubmitCount = 0;
      final exam = Exam(
        id: 1,
        title: 'IELTS Practice 1',
        description: '',
        premiumOnly: false,
        sections: const [],
      );

      await tester.pumpWidget(
        MaterialApp(
          home: SpeakingScreen(
            exam: exam,
            attempt: attempt(sessionId: 9),
            submitAttemptOverride: (attemptId) {
              submitCount++;
              return submitCompleter.future;
            },
            forceSubmitOverride: (attemptId) async {
              forceSubmitCount++;
              return attemptResult();
            },
            getSessionOverride: (sessionId) async {
              return MockSession.fromJson(mockSessionJson(status: 'COMPLETED'));
            },
          ),
        ),
      );

      await _tapManualSubmitAndConfirm(tester);
      expect(find.text('Đang nộp bài...'), findsOneWidget);

      await tester.pump(const Duration(seconds: 5));
      await tester.pump();

      expect(submitCount, 1);
      expect(forceSubmitCount, 0);

      submitCompleter.complete(attemptResult());
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));

      expect(submitCount, 1);
      expect(forceSubmitCount, 0);
      expect(find.text('Kết quả'), findsOneWidget);
    },
  );

  testWidgets(
    'speakingScreen_whenManualSubmitConfirmed_shouldSubmitOnceAndShowResult',
    (tester) async {
      var submitCount = 0;
      final exam = Exam(
        id: 1,
        title: 'IELTS Practice 1',
        description: '',
        premiumOnly: false,
        sections: const [],
      );

      await tester.pumpWidget(
        MaterialApp(
          home: SpeakingScreen(
            exam: exam,
            attempt: attempt(),
            submitAttemptOverride: (attemptId) async {
              submitCount++;
              return attemptResult();
            },
          ),
        ),
      );

      await _tapManualSubmitAndConfirm(tester);
      await tester.pump();

      expect(submitCount, 1);
      expect(find.text('Kết quả'), findsOneWidget);
    },
  );
}

Future<void> _pumpSpeakingScreen(
  WidgetTester tester,
  Exam exam, {
  Attempt? testAttempt,
  Future<void> Function(int attemptId, int questionId, String audioPath)?
  submitSpeaking,
  Future<AttemptResult> Function(int attemptId)? submitAttempt,
}) async {
  addTearDown(() async {
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump(const Duration(milliseconds: 100));
  });
  await tester.pumpWidget(
    MaterialApp(
      home: SpeakingScreen(
        exam: exam,
        attempt: testAttempt ?? attempt(),
        submitSpeakingOverride: submitSpeaking,
        submitAttemptOverride: submitAttempt,
      ),
    ),
  );
  await tester.pump();
}

Future<void> _startTimedAnswer(WidgetTester tester) async {
  await tester.tap(find.text('Bắt đầu trả lời'));
  await tester.pump();
  for (var i = 0; i < 10; i++) {
    await tester.pump(const Duration(milliseconds: 10));
    if (find.text('Đang ghi âm...').evaluate().isNotEmpty) {
      return;
    }
  }
}

Future<void> _finishTimedAnswer(WidgetTester tester) async {
  await tester.pump(const Duration(seconds: 1));
  for (var i = 0; i < 10; i++) {
    await tester.pump(const Duration(milliseconds: 30));
  }
}

Future<void> _tapManualSubmitAndConfirm(WidgetTester tester) async {
  await tester.tap(find.widgetWithText(FilledButton, 'Nộp bài'));
  await tester.pump();
  await tester.tap(find.text('Nộp bài').last);
  await tester.pump();
}

Exam speakingExam({required int questionCount}) {
  return Exam(
    id: 1,
    title: 'IELTS Practice 1',
    description: '',
    premiumOnly: false,
    sections: [
      ExamSection(
        id: 40,
        skillType: 'SPEAKING',
        passageContent: '',
        mediaUrl: '',
        sectionOrder: 4,
        questionCount: questionCount,
        questions: [
          for (var index = 0; index < questionCount; index++)
            Question(
              id: 401 + index,
              questionType: 'SPEAKING',
              content: 'Speak about topic ${index + 1}.',
              durationSeconds: 2,
              preparationSeconds: 1,
              orderIndex: index + 1,
              answers: const [],
            ),
        ],
      ),
    ],
  );
}

Map<String, dynamic> mockSessionJson({required String status}) {
  return {
    'id': 9,
    'roomCode': 'ROOM9',
    'examId': 1,
    'examTitle': 'IELTS Practice 1',
    'startTime': '2026-09-09T09:00:00',
    'endTime': '2026-09-09T12:00:00',
    'registrationDeadline': '2026-09-09T08:00:00',
    'maxCandidates': 10,
    'status': status,
    'registrationCount': 1,
  };
}

class FakePathProviderPlatform extends PathProviderPlatform {
  final String temporaryPath;

  FakePathProviderPlatform(this.temporaryPath);

  @override
  Future<String?> getTemporaryPath() async {
    return temporaryPath;
  }
}

class FakeRecordPlatform extends RecordPlatform {
  int startCount = 0;
  int stopCount = 0;
  String? _path;

  @override
  Future<void> create(String recorderId) async {}

  @override
  Future<void> start(
    String recorderId,
    RecordConfig config, {
    required String path,
  }) async {
    startCount++;
    _path = path;
  }

  @override
  Future<Stream<Uint8List>> startStream(
    String recorderId,
    RecordConfig config,
  ) async {
    return Stream<Uint8List>.empty();
  }

  @override
  Future<String?> stop(String recorderId) async {
    stopCount++;
    final path = _path;
    if (path == null) {
      return null;
    }
    final file = File(path);
    file.parent.createSync(recursive: true);
    file.writeAsBytesSync([1, 2, 3], flush: true);
    return path;
  }

  @override
  Future<void> pause(String recorderId) async {}

  @override
  Future<void> resume(String recorderId) async {}

  @override
  Future<bool> isRecording(String recorderId) async {
    return _path != null;
  }

  @override
  Future<bool> isPaused(String recorderId) async {
    return false;
  }

  @override
  Future<bool> hasPermission(String recorderId, {bool request = true}) async {
    return true;
  }

  @override
  Future<void> dispose(String recorderId) async {}

  @override
  Future<Amplitude> getAmplitude(String recorderId) async {
    return Amplitude(current: 0, max: 0);
  }

  @override
  Future<bool> isEncoderSupported(
    String recorderId,
    AudioEncoder encoder,
  ) async {
    return true;
  }

  @override
  Future<List<InputDevice>> listInputDevices(String recorderId) async {
    return const [];
  }

  @override
  Future<void> cancel(String recorderId) async {}

  @override
  Stream<RecordState> onStateChanged(String recorderId) {
    return const Stream<RecordState>.empty();
  }
}
