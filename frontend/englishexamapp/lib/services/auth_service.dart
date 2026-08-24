import '../models/user.dart';
import 'api_service.dart';

class AuthService {
  Future<void> register({
    required String username,
    required String email,
    required String password,
    required String fullName,
  }) async {
    await ApiService.dio.post(
      '/api/auth/register',
      data: {
        'username': username,
        'email': email,
        'password': password,
        'fullName': fullName,
      },
    );
  }

  Future<void> login({
    required String username,
    required String password,
  }) async {
    final response = await ApiService.dio.post(
      '/api/auth/login',
      data: {'username': username, 'password': password},
    );

    final data = response.data as Map<String, dynamic>;
    final token = data['accessToken'] as String?;
    if (token == null || token.isEmpty) {
      throw Exception('Backend không trả về accessToken.');
    }

    await saveToken(token);
  }

  Future<User> getCurrentUser() async {
    final response = await ApiService.dio.get('/api/users/me');
    return User.fromJson(response.data as Map<String, dynamic>);
  }

  Future<void> saveToken(String token) async {
    await ApiService.storage.write(key: 'accessToken', value: token);
  }

  Future<String?> getToken() async {
    return ApiService.storage.read(key: 'accessToken');
  }

  Future<void> deleteToken() async {
    await ApiService.storage.delete(key: 'accessToken');
  }

  Future<void> logout() async {
    await deleteToken();
  }

  Future<bool> isLoggedIn() async {
    final token = await getToken();
    return token != null && token.isNotEmpty;
  }
}
