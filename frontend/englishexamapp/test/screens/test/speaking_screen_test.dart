import 'package:englishexamapp/models/exam.dart';
import 'package:englishexamapp/screens/test/speaking_screen.dart';
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

  testWidgets('speakingScreen_whenNoSpeakingQuestions_shouldRenderEmptyStateAndSubmitButton', (tester) async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter(
      (options, body) => jsonResponse({'attemptId': 1}),
    );
    final exam = Exam(
      id: 1,
      title: 'IELTS Practice 1',
      description: '',
      premiumOnly: false,
      sections: const [],
    );

    await tester.pumpWidget(
      MaterialApp(home: SpeakingScreen(exam: exam, attempt: attempt())),
    );

    expect(find.text('Đề thi chưa có câu hỏi Speaking.'), findsOneWidget);
    expect(find.text('Nộp bài'), findsOneWidget);
  });
}
