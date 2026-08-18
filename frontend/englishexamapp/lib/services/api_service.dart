import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../config/api_config.dart';

class ApiService {
  static const FlutterSecureStorage storage = FlutterSecureStorage();

  static final Dio dio =
      Dio(
          BaseOptions(
            baseUrl: ApiConfig.baseUrl,
            connectTimeout: const Duration(seconds: 10),
            receiveTimeout: const Duration(seconds: 60),
            sendTimeout: const Duration(seconds: 60),
            headers: {'Content-Type': 'application/json'},
          ),
        )
        ..interceptors.add(
          InterceptorsWrapper(
            onRequest: (options, handler) async {
              final token = await storage.read(key: 'accessToken');
              if (token != null && token.isNotEmpty) {
                options.headers['Authorization'] = 'Bearer $token';
              }
              handler.next(options);
            },
          ),
        );

  static String getErrorMessage(Object error) {
    if (error is DioException) {
      final data = error.response?.data;
      final backendMessage = _extractBackendMessage(data);

      if (backendMessage != null) {
        return backendMessage;
      }

      switch (error.response?.statusCode) {
        case 400:
          return 'Du lieu gui len khong hop le.';
        case 401:
          return 'Phien dang nhap da het han. Vui long dang nhap lai.';
        case 403:
          return 'Ban khong co quyen thuc hien thao tac nay.';
        case 500:
          return 'May chu dang gap loi. Vui long thu lai sau.';
      }

      if (error.type == DioExceptionType.connectionTimeout ||
          error.type == DioExceptionType.receiveTimeout ||
          error.type == DioExceptionType.sendTimeout ||
          error.type == DioExceptionType.connectionError) {
        return 'Khong ket noi duoc backend. Kiem tra Spring Boot da chay chua.';
      }
    }

    return 'Da co loi xay ra. Vui long thu lai.';
  }

  static String? _extractBackendMessage(dynamic data) {
    if (data is Map<String, dynamic>) {
      final message = data['message'] ?? data['error'];
      if (message is String && message.isNotEmpty) {
        return message;
      }
    }

    if (data is String && data.isNotEmpty) {
      return data;
    }

    return null;
  }
}
