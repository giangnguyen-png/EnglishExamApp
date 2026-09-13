import 'package:englishexamapp/services/api_service.dart';
import 'package:englishexamapp/services/attempt_service.dart';
import 'package:englishexamapp/services/exam_service.dart';
import 'package:englishexamapp/services/expert_service.dart';
import 'package:englishexamapp/services/mock_session_service.dart';
import 'package:englishexamapp/services/payment_service.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../helpers/fake_dio_adapter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
  });

  test('examService_getExamDetail_whenResponseArrives_shouldMapExam', () async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter((options, body) {
      expect(options.path, '/api/exams/12');
      return jsonResponse({
        'id': 12,
        'title': 'Practice 12',
        'sections': [],
      });
    });

    final exam = await ExamService().getExamDetail(12);

    expect(exam.id, 12);
    expect(exam.title, 'Practice 12');
  });

  test('attemptService_getFreeQuota_whenResponseArrives_shouldMapQuota', () async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter((options, body) {
      expect(options.path, '/api/attempts/free-quota');
      return jsonResponse({'premium': false, 'limit': 5, 'used': 3, 'remaining': 2});
    });

    final quota = await AttemptService().getFreeQuota();

    expect(quota.premium, isFalse);
    expect(quota.remaining, 2);
  });

  test('paymentService_fakePremiumPayment_whenMessageMissing_shouldUseDefaultMessage', () async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter((options, body) {
      expect(options.method, 'POST');
      expect(options.path, '/api/payments/fake-premium');
      return jsonResponse({});
    });

    final message = await PaymentService().fakePremiumPayment();

    expect(message, 'Thanh toán thành công.');
  });

  test('mockSessionService_registerSession_whenResponseArrives_shouldMapRegistration', () async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter((options, body) {
      expect(options.path, '/api/mock-sessions/9/registrations');
      return jsonResponse({
        'registrationId': 20,
        'sessionId': 9,
        'candidateNumber': 4,
        'roomCode': 'ROOM-A',
        'examTitle': 'Premium Mock',
        'startTime': '2026-09-09T09:00:00',
        'endTime': '2026-09-09T12:00:00',
        'status': 'PENDING',
      });
    });

    final registration = await MockSessionService().registerSession(9);

    expect(registration.candidateNumber, 4);
    expect(registration.roomCode, 'ROOM-A');
  });

  test('expertService_createSession_whenDeadlineEmpty_shouldSendNullDeadline', () async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter((options, body) {
      expect(options.path, '/api/expert/mock-sessions');
      expect(body['registrationDeadline'], isNull);
      expect(body['maxCandidates'], 20);
      return jsonResponse({
        'id': 1,
        'roomCode': 'ROOM-A',
        'examId': 12,
        'examTitle': 'Premium Mock',
        'startTime': '2026-09-09T09:00:00',
        'endTime': '2026-09-09T12:00:00',
        'registrationDeadline': '',
        'maxCandidates': 20,
        'status': 'PENDING',
      });
    });

    final session = await ExpertService().createSession(
      examId: 12,
      roomCode: 'ROOM-A',
      startTime: '2026-09-09T09:00:00',
      endTime: '2026-09-09T12:00:00',
      registrationDeadline: '',
      maxCandidates: 20,
    );

    expect(session.roomCode, 'ROOM-A');
  });
}
