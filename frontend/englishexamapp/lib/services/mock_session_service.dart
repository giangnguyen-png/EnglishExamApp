import '../models/attempt.dart';
import '../models/mock_session.dart';
import 'api_service.dart';

class MockSessionService {
  Future<List<MockSession>> getAvailableSessions() async {
    final response = await ApiService.dio.get('/api/mock-sessions/available');
    final data = response.data as List<dynamic>;
    return data
        .map((item) => MockSession.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<SessionRegistration> registerSession(int sessionId) async {
    final response = await ApiService.dio.post(
      '/api/mock-sessions/$sessionId/registrations',
    );
    return SessionRegistration.fromJson(response.data as Map<String, dynamic>);
  }

  Future<List<SessionRegistration>> getMyRegistrations() async {
    final response = await ApiService.dio.get(
      '/api/mock-sessions/registrations/me',
    );
    final data = response.data as List<dynamic>;
    return data
        .map(
          (item) => SessionRegistration.fromJson(item as Map<String, dynamic>),
        )
        .toList();
  }

  Future<void> cancelRegistration(int registrationId) async {
    await ApiService.dio.delete(
      '/api/mock-sessions/registrations/$registrationId',
    );
  }

  Future<Attempt> startPremiumAttempt(int sessionId) async {
    final response = await ApiService.dio.post(
      '/api/mock-sessions/$sessionId/attempts',
    );
    return Attempt.fromJson(response.data as Map<String, dynamic>);
  }

  Future<MockSession> getSession(int sessionId) async {
    final response = await ApiService.dio.get('/api/mock-sessions/$sessionId');
    return MockSession.fromJson(response.data as Map<String, dynamic>);
  }
}
