import 'package:englishexamapp/models/attempt.dart';
import 'package:englishexamapp/models/exam.dart';
import 'package:englishexamapp/models/result.dart';
import 'package:englishexamapp/models/user.dart';

Attempt attempt({int attemptId = 1, int examId = 1, int? sessionId}) {
  return Attempt(
    attemptId: attemptId,
    examId: examId,
    sessionId: sessionId,
    startTime: '2026-09-09T09:00:00',
    status: 'IN_PROGRESS',
  );
}

Exam practiceExam() {
  return Exam(
    id: 1,
    title: 'IELTS Practice 1',
    description: 'Full practice test',
    premiumOnly: false,
    sections: [
      ExamSection(
        id: 10,
        skillType: 'LISTENING',
        passageContent: '',
        mediaUrl: '',
        sectionOrder: 1,
        questionCount: 2,
        questions: [
          question(id: 101, orderIndex: 1, content: 'Listen and choose A'),
          question(id: 102, orderIndex: 2, content: 'Listen and choose B'),
        ],
      ),
      ExamSection(
        id: 20,
        skillType: 'READING',
        passageContent: 'Reading passage',
        mediaUrl: '',
        sectionOrder: 2,
        questionCount: 1,
        questions: [
          question(id: 201, orderIndex: 1, content: 'Read and choose C'),
        ],
      ),
      ExamSection(
        id: 30,
        skillType: 'WRITING',
        passageContent: '',
        mediaUrl: '',
        sectionOrder: 3,
        questionCount: 1,
        questions: [
          Question(
            id: 301,
            questionType: 'WRITING',
            content: 'Write about the chart.',
            orderIndex: 1,
            answers: const [],
          ),
        ],
      ),
    ],
  );
}

Question question({
  required int id,
  required int orderIndex,
  required String content,
}) {
  return Question(
    id: id,
    questionType: 'SINGLE_CHOICE',
    content: content,
    orderIndex: orderIndex,
    answers: [
      AnswerOption(id: id * 10 + 1, content: 'Option A'),
      AnswerOption(id: id * 10 + 2, content: 'Option B'),
    ],
  );
}

AttemptResult attemptResult({double? speakingScore = 6.5}) {
  return AttemptResult(
    attemptId: 1,
    examId: 1,
    examTitle: 'IELTS Practice 1',
    startTime: '2026-09-09T09:00:00',
    endTime: '2026-09-09T12:00:00',
    overallBandScore: speakingScore == null ? null : 7.0,
    normalAttempt: speakingScore != null,
    aiOverallFeedback: '',
    overallFeedback: const AiFeedback(
      strengths: ['Good task coverage'],
      weaknesses: ['Needs more precise vocabulary'],
      improvements: ['Review mistakes weekly'],
    ),
    skills: [
      SkillResult(
        skillType: 'LISTENING',
        bandScore: 7.5,
        aiAnalysis: '',
        feedback: const AiFeedback(),
        writingTasks: const [],
      ),
      SkillResult(
        skillType: 'READING',
        bandScore: 7.0,
        aiAnalysis: '',
        feedback: const AiFeedback(),
        writingTasks: const [],
      ),
      SkillResult(
        skillType: 'WRITING',
        bandScore: 6.5,
        aiAnalysis: '',
        feedback: const AiFeedback(),
        writingTasks: const [],
      ),
      SkillResult(
        skillType: 'SPEAKING',
        bandScore: speakingScore,
        aiAnalysis: '',
        feedback: const AiFeedback(),
        writingTasks: const [],
      ),
    ],
  );
}

User user({String role = 'USER'}) {
  return User(
    id: 1,
    username: 'alice',
    email: 'alice@example.com',
    fullName: 'Alice Nguyen',
    role: role,
  );
}
