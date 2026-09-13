import 'package:dio/dio.dart';
import 'package:englishexamapp/services/api_service.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('ApiService.getErrorMessage', () {
    test('getErrorMessage_whenBackendMessageExists_shouldPreferBackendMessage', () {
      final error = DioException(
        requestOptions: RequestOptions(path: '/login'),
        response: Response(
          requestOptions: RequestOptions(path: '/login'),
          statusCode: 400,
          data: {'message': 'Sai tài khoản hoặc mật khẩu'},
        ),
      );

      expect(ApiService.getErrorMessage(error), 'Sai tài khoản hoặc mật khẩu');
    });

    test('getErrorMessage_whenForbiddenWithoutBody_shouldReturnPermissionMessage', () {
      final error = DioException(
        requestOptions: RequestOptions(path: '/expert'),
        response: Response(
          requestOptions: RequestOptions(path: '/expert'),
          statusCode: 403,
        ),
      );

      expect(
        ApiService.getErrorMessage(error),
        'Bạn không có quyền thực hiện thao tác này.',
      );
    });

    test('getErrorMessage_whenConnectionError_shouldReturnBackendConnectionMessage', () {
      final error = DioException(
        requestOptions: RequestOptions(path: '/api/exams'),
        type: DioExceptionType.connectionError,
        error: 'refused',
      );

      expect(
        ApiService.getErrorMessage(error),
        'Không kết nối được backend. Kiểm tra Spring Boot đã chạy chưa.',
      );
    });
  });
}
