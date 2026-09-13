import 'package:englishexamapp/screens/result/result_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/test_data.dart';

void main() {
  testWidgets('resultScreen_whenScoresExist_shouldRenderOverallAndFourSkills', (tester) async {
    await tester.pumpWidget(
      MaterialApp(home: ResultScreen(result: attemptResult())),
    );

    expect(find.text('Overall Band'), findsOneWidget);
    expect(find.text('7.0'), findsWidgets);
    expect(find.text('Listening'), findsOneWidget);
    expect(find.text('Reading'), findsOneWidget);
    expect(find.text('Writing'), findsOneWidget);
    expect(find.text('Speaking'), findsOneWidget);
  });

  testWidgets('resultScreen_whenSpeakingIsAwaitingExpert_shouldRenderWaitingMessage', (tester) async {
    await tester.pumpWidget(
      MaterialApp(home: ResultScreen(result: attemptResult(speakingScore: null))),
    );

    expect(find.text('Chưa có kết quả'), findsOneWidget);
    expect(find.text('Chờ giám khảo'), findsOneWidget);
    expect(
      find.textContaining('Speaking đang chờ giám khảo chấm'),
      findsOneWidget,
    );
  });
}
