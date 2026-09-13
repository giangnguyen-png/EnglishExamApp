import 'package:englishexamapp/models/exam.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('Exam.fromJson', () {
    test('fromJson_whenOptionalFieldsAreMissing_shouldUseDefaults', () {
      final exam = Exam.fromJson({
        'id': 1,
        'title': 'IELTS Practice 1',
        'sections': [
          {
            'id': 10,
            'skillType': 'SPEAKING',
            'questions': [
              {
                'id': 100,
                'questionType': 'SPEAKING',
                'imageUrl': '   ',
                'durationSeconds': '45',
                'preparationSeconds': 15.8,
                'answers': [
                  {'id': 1},
                ],
              },
            ],
          },
        ],
      });

      expect(exam.description, isEmpty);
      expect(exam.premiumOnly, isFalse);
      expect(exam.sections.single.passageContent, isEmpty);
      expect(exam.sections.single.questionCount, 1);
      expect(exam.sections.single.questions.single.content, isEmpty);
      expect(exam.sections.single.questions.single.imageUrl, isNull);
      expect(exam.sections.single.questions.single.durationSeconds, 45);
      expect(exam.sections.single.questions.single.preparationSeconds, 15);
      expect(exam.sections.single.questions.single.answers.single.content, isEmpty);
    });

    test('fromJson_whenQuestionsOutOfOrder_shouldPreserveApiOrder', () {
      final exam = Exam.fromJson({
        'id': 1,
        'title': 'IELTS Practice 1',
        'description': 'Full test',
        'premiumOnly': true,
        'sections': [
          {
            'id': 10,
            'skillType': 'READING',
            'passageContent': 'Passage',
            'mediaUrl': '',
            'sectionOrder': 2,
            'questionCount': 2,
            'questions': [
              {
                'id': 2,
                'questionType': 'SINGLE_CHOICE',
                'content': 'Second',
                'orderIndex': 2,
                'answers': [],
              },
              {
                'id': 1,
                'questionType': 'SINGLE_CHOICE',
                'content': 'First',
                'orderIndex': 1,
                'answers': [],
              },
            ],
          },
        ],
      });

      expect(exam.premiumOnly, isTrue);
      expect(exam.sections.single.questions.first.content, 'Second');
      expect(exam.sections.single.questions.last.content, 'First');
    });
  });
}
