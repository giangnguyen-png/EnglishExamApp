import 'package:englishexamapp/screens/test/test_screen.dart';
import 'package:englishexamapp/screens/test/writing_screen.dart';
import 'package:englishexamapp/services/api_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_dio_adapter.dart';
import '../../helpers/test_data.dart';

void main() {
  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
  });

  testWidgets(
    'testScreen_whenAnswerSelected_shouldSaveAnswerAndUpdateAnsweredCount',
    (tester) async {
      ApiService.dio.httpClientAdapter = FakeDioAdapter((options, body) {
        expect(options.method, 'PUT');
        expect(options.path, '/api/attempts/1/questions/101/answer');
        expect(body, {
          'answerIds': [1011],
        });
        return jsonResponse({});
      });

      await tester.pumpWidget(
        MaterialApp(
          home: TestScreen(exam: practiceExam(), attempt: attempt()),
        ),
      );

      expect(find.text('Listen and choose A'), findsOneWidget);
      expect(find.text('0/2 đã làm'), findsOneWidget);

      await tester.tap(find.text('Option A').first);
      await tester.pump();
      await tester.pump(const Duration(seconds: 1));

      expect(find.text('1/2 đã làm'), findsOneWidget);
    },
  );

  testWidgets(
    'testScreen_whenNextAndPreviousTapped_shouldMoveBetweenQuestions',
    (tester) async {
      ApiService.dio.httpClientAdapter = FakeDioAdapter(
        (options, body) => jsonResponse({}),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: TestScreen(exam: practiceExam(), attempt: attempt()),
        ),
      );

      expect(find.text('Listen and choose A'), findsOneWidget);
      await tester.tap(find.text('Sau'));
      await tester.pump();

      expect(find.text('Listen and choose B'), findsOneWidget);
      await tester.tap(find.text('Trước'));
      await tester.pump();

      expect(find.text('Listen and choose A'), findsOneWidget);
    },
  );

  testWidgets('testScreen_whenReturningToSkill_shouldKeepRemainingTime', (
    tester,
  ) async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter(
      (options, body) => jsonResponse({}),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: TestScreen(exam: practiceExam(), attempt: attempt()),
      ),
    );

    await tester.pump(const Duration(seconds: 5));
    expect(find.text('Thời gian còn lại: 29:55'), findsOneWidget);

    await tester.tap(find.text('Sau'));
    await tester.pump();
    await tester.tap(find.text('Sau'));
    await tester.pump();

    expect(find.text('Read and choose C'), findsOneWidget);
    expect(find.text('Thời gian còn lại: 60:00'), findsOneWidget);

    await tester.scrollUntilVisible(
      find.text('Trước'),
      240,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.tap(find.text('Trước'));
    await tester.pump();

    expect(find.text('Listen and choose B'), findsOneWidget);
    expect(find.text('Thời gian còn lại: 29:55'), findsOneWidget);
  });

  testWidgets('testScreen_whenListeningExpires_shouldLockListening', (
    tester,
  ) async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter(
      (options, body) => jsonResponse({}),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: TestScreen(exam: practiceExam(), attempt: attempt()),
      ),
    );

    await tester.pump(const Duration(minutes: 30));
    await tester.pump();

    expect(find.text('Read and choose C'), findsOneWidget);
    expect(find.text('Thời gian còn lại: 60:00'), findsOneWidget);

    await tester.scrollUntilVisible(
      find.text('Trước'),
      240,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.tap(find.text('Trước'));
    await tester.pump();

    expect(find.text('Read and choose C'), findsOneWidget);
    expect(find.text('Listen and choose B'), findsNothing);
  });

  testWidgets('testScreen_whenReadingExpires_shouldOpenWritingAndLockReading', (
    tester,
  ) async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter(
      (options, body) => jsonResponse({}),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: TestScreen(exam: practiceExam(), attempt: attempt()),
      ),
    );

    await tester.tap(find.text('Sau'));
    await tester.pump();
    await tester.tap(find.text('Sau'));
    await tester.pump();
    expect(find.text('Read and choose C'), findsOneWidget);

    await tester.pump(const Duration(minutes: 60));
    await tester.pump();

    expect(find.byType(WritingScreen), findsOneWidget);
    expect(find.text('Writing Task 1'), findsOneWidget);
    expect(
      Navigator.of(tester.element(find.byType(WritingScreen))).canPop(),
      isFalse,
    );
  });
}
