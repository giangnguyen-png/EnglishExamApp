import 'dart:convert';

import 'package:englishexamapp/models/result.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('AttemptResult.fromJson', () {
    test('fromJson_whenAiJsonIsPresent_shouldMapFeedbackAndWritingTasks', () {
      final result = AttemptResult.fromJson({
        'attemptId': 7,
        'examId': 1,
        'examTitle': 'IELTS Practice 1',
        'startTime': '2026-01-01T09:00:00',
        'endTime': '2026-01-01T12:00:00',
        'overallBandScore': '7.5',
        'normalAttempt': false,
        'aiOverallFeedback': jsonEncode({
          'strengths': ['Clear ideas', '  '],
          'weaknesses': ['Vocabulary'],
          'improvements': ['Practice timing'],
        }),
        'skills': [
          {
            'skillType': 'WRITING',
            'bandScore': 7,
            'aiAnalysis': jsonEncode([
              {
                'questionId': '101',
                'score': '6.5',
                'feedback': {
                  'strengths': ['Task response'],
                  'weaknesses': ['Grammar'],
                  'improvements': ['Use examples'],
                },
              },
            ]),
          },
          {
            'skillType': 'SPEAKING',
            'bandScore': null,
            'aiAnalysis': jsonEncode({
              'feedback': {
                'strengths': ['Fluent'],
                'weaknesses': [],
                'improvements': ['Pronunciation'],
              },
            }),
          },
        ],
      });

      expect(result.overallBandScore, 7.5);
      expect(result.normalAttempt, isFalse);
      expect(result.overallFeedback.strengths, ['Clear ideas']);
      expect(result.skillByType('READING'), isNull);

      final writing = result.skillByType('WRITING')!;
      expect(writing.bandScore, 7.0);
      expect(writing.writingTasks.single.questionId, 101);
      expect(writing.writingTasks.single.score, 6.5);
      expect(writing.writingTasks.single.feedback.improvements, ['Use examples']);

      final speaking = result.skillByType('SPEAKING')!;
      expect(speaking.bandScore, isNull);
      expect(speaking.feedback.strengths, ['Fluent']);
    });

    test('fromJson_whenAiAnalysisIsPlainText_shouldKeepReadableText', () {
      final skill = SkillResult.fromJson({
        'skillType': 'READING',
        'bandScore': '6.0',
        'aiAnalysis': 'Good objective result',
      });

      expect(skill.bandScore, 6.0);
      expect(skill.aiAnalysis, 'Good objective result');
      expect(skill.feedback.isEmpty, isTrue);
      expect(skill.writingTasks, isEmpty);
    });
  });
}
