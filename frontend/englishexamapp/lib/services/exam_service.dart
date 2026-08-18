import '../models/exam.dart';
import 'api_service.dart';

class ExamService {
  Future<List<Exam>> getExams() async {
    final response = await ApiService.dio.get('/api/exams');
    final data = response.data as List<dynamic>;
    return data
        .map((item) => Exam.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<Exam> getExamDetail(int examId) async {
    final response = await ApiService.dio.get('/api/exams/$examId');
    return Exam.fromJson(response.data as Map<String, dynamic>);
  }
}
