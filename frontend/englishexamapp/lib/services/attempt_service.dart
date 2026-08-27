import '../models/attempt.dart';
import '../models/exam.dart';
import '../models/result.dart';
import 'api_service.dart';

class AttemptService {
  Future<Attempt> startAttempt(int examId) async {
    final response = await ApiService.dio.post('/api/exams/$examId/attempts');
    return Attempt.fromJson(response.data as Map<String, dynamic>);
  }

  Future<AttemptResult> submitAttempt(int attemptId) async {
    final response = await ApiService.dio.post(
      '/api/attempts/$attemptId/submit',
    );
    return AttemptResult.fromJson(response.data as Map<String, dynamic>);
  }

  Future<AttemptResult> forceSubmitAttempt(int attemptId) async {
    final response = await ApiService.dio.post(
      '/api/attempts/$attemptId/force-submit',
    );
    return AttemptResult.fromJson(response.data as Map<String, dynamic>);
  }

  Future<AttemptResult> getAttemptResult(int attemptId) async {
    final response = await ApiService.dio.get('/api/attempts/$attemptId');
    return AttemptResult.fromJson(response.data as Map<String, dynamic>);
  }

  Future<Exam> getAttemptExam(int attemptId) async {
    final response = await ApiService.dio.get('/api/attempts/$attemptId/exam');
    return Exam.fromJson(response.data as Map<String, dynamic>);
  }

  Future<List<AttemptHistory>> getAttemptHistory() async {
    final response = await ApiService.dio.get('/api/attempts');
    final data = response.data as List<dynamic>;
    return data
        .map((item) => AttemptHistory.fromJson(item as Map<String, dynamic>))
        .toList();
  }
}
