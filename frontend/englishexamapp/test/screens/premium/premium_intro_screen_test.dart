import 'package:englishexamapp/screens/premium/premium_intro_screen.dart';
import 'package:englishexamapp/services/api_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_dio_adapter.dart';

void main() {
  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
  });

  testWidgets('premiumIntro_whenUserIsPremium_shouldRenderExpiryAndSessionButton', (tester) async {
    ApiService.dio.httpClientAdapter = FakeDioAdapter((options, body) {
      expect(options.path, '/api/payments/premium-status');
      return jsonResponse({
        'premium': true,
        'expiresAt': '2026-10-09T00:00:00',
        'message': 'Active',
      });
    });

    await tester.pumpWidget(const MaterialApp(home: PremiumIntroScreen()));
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));

    expect(find.text('Premium đang hoạt động'), findsOneWidget);
    expect(find.text('Hết hạn: 09/10/2026'), findsOneWidget);
    expect(find.text('Vào ca thi Premium'), findsOneWidget);
  });
}
