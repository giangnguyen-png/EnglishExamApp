import 'package:englishexamapp/models/attempt_review.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('AttemptReview.fromJson', () {
    test('skillTypes_whenSectionsRepeatSkills_shouldReturnUniqueOrderedSkills', () {
      final review = AttemptReview.fromJson({
        'attemptId': 1,
        'examTitle': 'Practice',
        'sections': [
          {'id': 1, 'skillType': 'LISTENING', 'questions': []},
          {'id': 2, 'skillType': 'READING', 'questions': []},
          {'id': 3, 'skillType': 'LISTENING', 'questions': []},
        ],
      });

      expect(review.skillTypes, ['LISTENING', 'READING']);
    });

    test('fromJson_whenQuestionHasSparseResponse_shouldMapDefaults', () {
      final review = AttemptReview.fromJson({
        'attemptId': 2,
        'examTitle': 'Practice',
        'sections': [
          {
            'id': 1,
            'skillType': 'READING',
            'questions': [
              {
                'id': 10,
                'content': 'Question',
                'questionType': 'SINGLE_CHOICE',
                'imageUrl': ' ',
                'answered': true,
                'correct': false,
                'aiScore': '5.5',
                'answers': [
                  {
                    'id': 99,
                    'content': 'A',
                    'selected': true,
                    'correct': false,
                  },
                ],
              },
            ],
          },
        ],
      });

      final question = review.sections.single.questions.single;
      expect(question.imageUrl, isNull);
      expect(question.answered, isTrue);
      expect(question.correct, isFalse);
      expect(question.aiScore, 5.5);
      expect(question.textResponse, isEmpty);
      expect(question.answers.single.explanation, isEmpty);
    });
  });
}
