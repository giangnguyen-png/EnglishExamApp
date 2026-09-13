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

  testWidgets('writingScreen_whenEssayTextChanges_shouldUpdateWordCount', (tester) async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter(
      (options, body) => jsonResponse({}),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: WritingScreen(exam: practiceExam(), attempt: attempt()),
      ),
    );

    expect(find.text('Writing Task 1'), findsOneWidget);
    expect(find.text('Số từ: 0'), findsOneWidget);

    await tester.enterText(
      find.byType(TextField),
      'This essay has five words',
    );
    await tester.pump();

    expect(find.text('Số từ: 5'), findsOneWidget);
  });

  testWidgets('writingScreen_whenSavingBlankWriting_shouldShowValidationSnackBar', (tester) async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter(
      (options, body) => jsonResponse({}),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: WritingScreen(exam: practiceExam(), attempt: attempt()),
      ),
    );

    await tester.scrollUntilVisible(
      find.text('Lưu bài Writing'),
      240,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.tap(find.text('Lưu bài Writing'));
    await tester.pump();

    expect(
      find.text('Vui lòng nhập bài Writing Task 1.'),
      findsOneWidget,
    );
  });
}
