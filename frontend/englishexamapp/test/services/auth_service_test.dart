import 'package:englishexamapp/services/api_service.dart';
import 'package:englishexamapp/services/auth_service.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../helpers/fake_dio_adapter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
  });

  test('login_whenBackendReturnsToken_shouldPersistTokenAndSendCredentials', () async {
    late FakeDioAdapter adapter;
    adapter = FakeDioAdapter((options, body) {
      expect(options.method, 'POST');
      expect(options.path, '/api/auth/login');
      expect(body, {'username': 'alice', 'password': 'secret'});
      return jsonResponse({'accessToken': 'jwt-token'});
    });
    ApiService.dio.httpClientAdapter = adapter;

    await AuthService().login(username: 'alice', password: 'secret');

    expect(await AuthService().getToken(), 'jwt-token');
    expect(adapter.requests.single.path, '/api/auth/login');
  });

  test('login_whenTokenIsMissing_shouldThrowAndNotPersistToken', () async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter(
      (options, body) => jsonResponse({}),
    );

    await expectLater(
      AuthService().login(username: 'alice', password: 'secret'),
      throwsException,
    );
    expect(await AuthService().getToken(), isNull);
  });
}
