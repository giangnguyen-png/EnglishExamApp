import 'package:englishexamapp/screens/home_screen.dart';
import 'package:englishexamapp/screens/auth/login_screen.dart';
import 'package:englishexamapp/services/api_service.dart';
import 'package:englishexamapp/services/auth_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_dio_adapter.dart';

void main() {
  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
  });

  testWidgets('login_whenFieldsAreEmpty_shouldShowValidationMessages', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: LoginScreen()));

    await tester.tap(find.byType(FilledButton));
    await tester.pump();

    expect(find.text('Vui lòng nhập tên đăng nhập'), findsOneWidget);
    expect(find.text('Vui lòng nhập mật khẩu'), findsOneWidget);
  });

  testWidgets('login_whenBackendRejectsCredentials_shouldShowErrorMessage', (tester) async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter(
      (options, body) => jsonResponse(
        {'message': 'Sai tài khoản hoặc mật khẩu'},
        statusCode: 401,
      ),
    );

    await tester.pumpWidget(const MaterialApp(home: LoginScreen()));
    await tester.enterText(find.byType(TextFormField).at(0), 'alice');
    await tester.enterText(find.byType(TextFormField).at(1), 'bad-password');
    await tester.tap(find.byType(FilledButton));
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));

    expect(find.text('Sai tài khoản hoặc mật khẩu'), findsOneWidget);
  });

  testWidgets('login_whenBackendReturnsToken_shouldNavigateToHome', (tester) async {
    tester.view.physicalSize = const Size(900, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    ApiService.dio.httpClientAdapter = FakeDioAdapter((options, body) {
      if (options.path == '/api/auth/login') {
        return jsonResponse({'accessToken': 'jwt-token'});
      }
      if (options.path == '/api/users/me') {
        return jsonResponse({
          'id': 1,
          'username': 'alice',
          'email': 'alice@example.com',
          'fullName': 'Alice Nguyen',
          'role': 'USER',
        });
      }
      return jsonResponse({}, statusCode: 404);
    });

    await tester.pumpWidget(const MaterialApp(home: LoginScreen()));
    await tester.enterText(find.byType(TextFormField).at(0), 'alice');
    await tester.enterText(find.byType(TextFormField).at(1), 'secret');
    await tester.tap(find.byType(FilledButton));
    await tester.pump();
    await tester.pumpAndSettle();

    expect(await AuthService().getToken(), 'jwt-token');
    expect(find.byType(LoginScreen), findsNothing);
    expect(find.byType(HomeScreen), findsOneWidget);
  });
}
