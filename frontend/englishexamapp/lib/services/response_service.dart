import 'package:dio/dio.dart';

import 'api_service.dart';

class ResponseService {
  Future<void> saveAnswer(
    int attemptId,
    int questionId,
    List<int> answerIds,
  ) async {
    await ApiService.dio.put(
      '/api/attempts/$attemptId/questions/$questionId/answer',
      data: {'answerIds': answerIds},
    );
  }

  Future<void> submitWriting(
    int attemptId,
    int questionId,
    String textContent,
  ) async {
    await ApiService.dio.put(
      '/api/attempts/$attemptId/questions/$questionId/writing',
      data: {'textContent': textContent},
    );
  }

  Future<void> saveWritingDraft(
    int attemptId,
    int questionId,
    String textContent,
  ) async {
    await ApiService.dio.put(
      '/api/attempts/$attemptId/questions/$questionId/writing-draft',
      data: {'textContent': textContent},
    );
  }

  Future<void> submitSpeaking(
    int attemptId,
    int questionId,
    String audioFilePath,
  ) async {
    final formData = FormData.fromMap({
      'audioFile': await MultipartFile.fromFile(audioFilePath),
    });

    await ApiService.dio.put(
      '/api/attempts/$attemptId/questions/$questionId/speaking',
      data: formData,
      options: Options(
        sendTimeout: const Duration(seconds: 180),
        receiveTimeout: const Duration(seconds: 180),
      ),
    );
  }
}
