import 'package:englishexamapp/widgets/state_views.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('formatDateTime_whenValidIsoString_shouldFormatDateAndTime', () {
    expect(formatDateTime('2026-09-09T16:05:00'), '09/09/2026 • 16:05');
  });

  test('formatDateTime_whenInputIsEmptyOrInvalid_shouldReturnFallback', () {
    expect(formatDateTime(''), 'Chưa có');
    expect(formatDateTime('not-a-date'), 'not-a-date');
  });

  testWidgets('ErrorView_whenRetryTapped_shouldCallCallback', (tester) async {
    var retries = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: ErrorView(
          message: 'Không tải được dữ liệu',
          onRetry: () => retries++,
        ),
      ),
    );

    await tester.tap(find.text('Thử lại'));

    expect(retries, 1);
  });
}
