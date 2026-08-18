import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:englishexamapp/main.dart';

void main() {
  testWidgets('App starts with a loading screen', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());

    expect(find.byType(MaterialApp), findsOneWidget);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}
