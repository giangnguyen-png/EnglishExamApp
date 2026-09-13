import 'package:englishexamapp/screens/exam/exam_detail_screen.dart';
import 'package:englishexamapp/services/api_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_dio_adapter.dart';

void main() {
  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
  });

  testWidgets('examDetail_whenNormalExamLoads_shouldRenderExamInfoAndStartButton', (tester) async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter((options, body) {
      if (options.path == '/api/exams/1') {
        return jsonResponse({
          'id': 1,
          'title': 'IELTS Practice 1',
          'description': 'Full practice test',
          'premiumOnly': false,
          'sections': [
            {
              'id': 10,
              'skillType': 'LISTENING',
              'questionCount': 40,
              'questions': [],
            },
          ],
        });
      }
      if (options.path == '/api/attempts/free-quota') {
        return jsonResponse({'premium': false, 'limit': 5, 'used': 2, 'remaining': 3});
      }
      return jsonResponse({}, statusCode: 404);
    });

    await tester.pumpWidget(const MaterialApp(home: ExamDetailScreen(examId: 1)));
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));

    expect(find.text('IELTS Practice 1'), findsOneWidget);
    expect(find.text('Full practice test'), findsOneWidget);
    expect(find.text('Lượt luyện tập miễn phí: 2/5'), findsOneWidget);
    expect(find.text('Bắt đầu luyện tập'), findsOneWidget);
    expect(find.text('40 câu hỏi'), findsOneWidget);
  });

  testWidgets('examDetail_whenFreeQuotaIsUsedUp_shouldOfferPremiumUpgrade', (tester) async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter((options, body) {
      if (options.path == '/api/exams/1') {
        return jsonResponse({
          'id': 1,
          'title': 'IELTS Practice 1',
          'sections': [],
        });
      }
      return jsonResponse({'premium': false, 'limit': 5, 'used': 5, 'remaining': 0});
    });

    await tester.pumpWidget(const MaterialApp(home: ExamDetailScreen(examId: 1)));
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));

    expect(find.text('Nâng cấp Premium'), findsOneWidget);
    expect(find.text('Bắt đầu luyện tập'), findsNothing);
  });
}
