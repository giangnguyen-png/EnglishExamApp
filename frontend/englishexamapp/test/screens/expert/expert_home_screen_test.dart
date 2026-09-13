import 'package:englishexamapp/screens/expert/expert_home_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/test_data.dart';

void main() {
  testWidgets('expertHome_whenExpertUserProvided_shouldRenderExpertActions', (tester) async {
    await tester.pumpWidget(
      MaterialApp(home: ExpertHomeScreen(user: user(role: 'EXPERT'))),
    );

    expect(find.text('Xin chào, Alice Nguyen'), findsOneWidget);
    expect(find.text('Expert'), findsOneWidget);
    expect(find.text('Quản lý ca thi'), findsOneWidget);
  });
}
