import 'package:englishexamapp/screens/premium/session_list_screen.dart';
import 'package:englishexamapp/services/api_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_dio_adapter.dart';

void main() {
  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
  });

  testWidgets('sessionList_whenSessionsLoad_shouldRenderSessionAndRegisterButton', (tester) async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter((options, body) {
      if (options.path == '/api/mock-sessions/available') {
        return jsonResponse([
          {
            'id': 5,
            'roomCode': 'ROOM-A',
            'examId': 1,
            'examTitle': 'Premium Mock 1',
            'startTime': '2026-09-09T09:00:00',
            'endTime': '2026-09-09T12:00:00',
            'registrationDeadline': '2026-09-08T22:00:00',
            'maxCandidates': 20,
            'status': 'PENDING',
            'registrationCount': 3,
          },
        ]);
      }
      return jsonResponse({
        'registrationId': 10,
        'sessionId': 5,
        'candidateNumber': 7,
        'roomCode': 'ROOM-A',
        'examTitle': 'Premium Mock 1',
        'startTime': '2026-09-09T09:00:00',
        'endTime': '2026-09-09T12:00:00',
        'status': 'PENDING',
      });
    });

    await tester.pumpWidget(const MaterialApp(home: SessionListScreen()));
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));

    expect(find.text('Premium Mock 1'), findsOneWidget);
    expect(find.text('Phòng: ROOM-A'), findsOneWidget);
    expect(find.text('Sắp diễn ra'), findsOneWidget);

    await tester.tap(find.text('Đăng ký'));
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));

    expect(find.text('Đăng ký thành công. Số báo danh: 7'), findsOneWidget);
  });
}
