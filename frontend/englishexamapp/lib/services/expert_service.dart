import '../models/mock_session.dart';
import '../models/result.dart';
import 'api_service.dart';

class ExpertService {
  Future<MockSession> createSession({
    required int examId,
    required String roomCode,
    required String startTime,
    required String endTime,
    required String registrationDeadline,
    required int maxCandidates,
  }) async {
    final response = await ApiService.dio.post(
      '/api/expert/mock-sessions',
      data: _sessionData(
        examId: examId,
        roomCode: roomCode,
        startTime: startTime,
        endTime: endTime,
        registrationDeadline: registrationDeadline,
        maxCandidates: maxCandidates,
      ),
    );
    return MockSession.fromJson(response.data as Map<String, dynamic>);
  }

  Future<List<MockSession>> getMySessions() async {
    final response = await ApiService.dio.get('/api/expert/mock-sessions');
    final data = response.data as List<dynamic>;
    return data
        .map((item) => MockSession.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<MockSession> getSessionDetail(int sessionId) async {
    final response = await ApiService.dio.get(
      '/api/expert/mock-sessions/$sessionId',
    );
    return MockSession.fromJson(response.data as Map<String, dynamic>);
  }

  Future<MockSession> updateSession({
    required int sessionId,
    required int examId,
    required String roomCode,
    required String startTime,
    required String endTime,
    required String registrationDeadline,
    required int maxCandidates,
  }) async {
    final response = await ApiService.dio.put(
      '/api/expert/mock-sessions/$sessionId',
      data: _sessionData(
        examId: examId,
        roomCode: roomCode,
        startTime: startTime,
        endTime: endTime,
        registrationDeadline: registrationDeadline,
        maxCandidates: maxCandidates,
      ),
    );
    return MockSession.fromJson(response.data as Map<String, dynamic>);
  }

  Future<void> deleteSession(int sessionId) async {
    await ApiService.dio.delete('/api/expert/mock-sessions/$sessionId');
  }

  Future<MockSession> startSession(int sessionId) async {
    final response = await ApiService.dio.post(
      '/api/expert/mock-sessions/$sessionId/start',
    );
    return MockSession.fromJson(response.data as Map<String, dynamic>);
  }

  Future<MockSession> finishSession(int sessionId) async {
    final response = await ApiService.dio.post(
      '/api/expert/mock-sessions/$sessionId/finish',
    );
    return MockSession.fromJson(response.data as Map<String, dynamic>);
  }

  Future<List<ExpertRegistration>> getRegistrations(int sessionId) async {
    final response = await ApiService.dio.get(
      '/api/expert/mock-sessions/$sessionId/registrations',
    );
    final data = response.data as List<dynamic>;
    return data
        .map(
          (item) => ExpertRegistration.fromJson(item as Map<String, dynamic>),
        )
        .toList();
  }

  Future<List<SpeakingAttempt>> getSpeakingAttempts(int sessionId) async {
    final response = await ApiService.dio.get(
      '/api/expert/mock-sessions/$sessionId/speaking-attempts',
    );
    final data = response.data as List<dynamic>;
    return data
        .map((item) => SpeakingAttempt.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<AttemptResult> gradeSpeaking(int attemptId, double score) async {
    final response = await ApiService.dio.put(
      '/api/expert/attempts/$attemptId/speaking-score',
      data: {'score': score},
    );
    return AttemptResult.fromJson(response.data as Map<String, dynamic>);
  }

  Map<String, dynamic> _sessionData({
    required int examId,
    required String roomCode,
    required String startTime,
    required String endTime,
    required String registrationDeadline,
    required int maxCandidates,
  }) {
    return {
      'examId': examId,
      'roomCode': roomCode,
      'startTime': startTime,
      'endTime': endTime,
      'registrationDeadline': registrationDeadline.isEmpty
          ? null
          : registrationDeadline,
      'maxCandidates': maxCandidates,
    };
  }
}
