import 'package:englishexamapp/config/ielts_time.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('forSkill_whenKnownSkill_shouldReturnOfficialPracticeDuration', () {
    expect(IeltsTime.forSkill('LISTENING'), const Duration(minutes: 30));
    expect(IeltsTime.forSkill('READING'), const Duration(minutes: 60));
    expect(IeltsTime.forSkill('WRITING'), const Duration(minutes: 60));
  });

  test('forSkill_whenUnknownSkill_shouldReturnZero', () {
    expect(IeltsTime.forSkill('SPEAKING'), Duration.zero);
    expect(IeltsTime.forSkill('UNKNOWN'), Duration.zero);
  });
}
