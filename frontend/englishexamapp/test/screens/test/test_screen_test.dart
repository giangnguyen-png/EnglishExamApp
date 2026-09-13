import 'package:englishexamapp/screens/test/test_screen.dart';
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

  testWidgets('testScreen_whenAnswerSelected_shouldSaveAnswerAndUpdateAnsweredCount', (tester) async {
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
  });

  testWidgets('testScreen_whenNextAndPreviousTapped_shouldMoveBetweenQuestions', (tester) async {
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
  });
}
