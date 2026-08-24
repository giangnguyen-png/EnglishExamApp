class Exam {
  final int id;
  final String title;
  final String description;
  final bool premiumOnly;
  final List<ExamSection> sections;

  Exam({
    required this.id,
    required this.title,
    required this.description,
    required this.premiumOnly,
    required this.sections,
  });

  factory Exam.fromJson(Map<String, dynamic> json) {
    final sectionsJson = (json['sections'] as List<dynamic>?) ?? [];

    return Exam(
      id: json['id'] as int,
      title: json['title'] as String,
      description: (json['description'] as String?) ?? '',
      premiumOnly: (json['premiumOnly'] as bool?) ?? false,
      sections: sectionsJson
          .map((item) => ExamSection.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}

class ExamSection {
  final int id;
  final String skillType;
  final String passageContent;
  final String mediaUrl;
  final int sectionOrder;
  final int questionCount;
  final List<Question> questions;

  ExamSection({
    required this.id,
    required this.skillType,
    required this.passageContent,
    required this.mediaUrl,
    required this.sectionOrder,
    required this.questionCount,
    required this.questions,
  });

  factory ExamSection.fromJson(Map<String, dynamic> json) {
    final questionsJson = (json['questions'] as List<dynamic>?) ?? [];

    return ExamSection(
      id: json['id'] as int,
      skillType: json['skillType'] as String,
      passageContent: (json['passageContent'] as String?) ?? '',
      mediaUrl: (json['mediaUrl'] as String?) ?? '',
      sectionOrder: (json['sectionOrder'] as int?) ?? 0,
      questionCount: (json['questionCount'] as int?) ?? questionsJson.length,
      questions: questionsJson
          .map((item) => Question.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}

class Question {
  final int id;
  final String questionType;
  final String content;
  final String? imageUrl;
  final int? durationSeconds;
  final int? preparationSeconds;
  final int orderIndex;
  final List<AnswerOption> answers;

  Question({
    required this.id,
    required this.questionType,
    required this.content,
    this.imageUrl,
    this.durationSeconds,
    this.preparationSeconds,
    required this.orderIndex,
    required this.answers,
  });

  factory Question.fromJson(Map<String, dynamic> json) {
    final answersJson = (json['answers'] as List<dynamic>?) ?? [];

    return Question(
      id: json['id'] as int,
      questionType: json['questionType'] as String,
      content: (json['content'] as String?) ?? '',
      imageUrl: _readOptionalString(json['imageUrl']),
      durationSeconds: _toInt(json['durationSeconds']),
      preparationSeconds: _toInt(json['preparationSeconds']),
      orderIndex: (json['orderIndex'] as int?) ?? 0,
      answers: answersJson
          .map((item) => AnswerOption.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}

String? _readOptionalString(dynamic value) {
  final text = value?.toString().trim();
  return text == null || text.isEmpty ? null : text;
}

int? _toInt(dynamic value) {
  if (value == null) {
    return null;
  }
  if (value is int) {
    return value;
  }
  if (value is num) {
    return value.toInt();
  }
  return int.tryParse(value.toString());
}

class AnswerOption {
  final int id;
  final String content;

  AnswerOption({required this.id, required this.content});

  factory AnswerOption.fromJson(Map<String, dynamic> json) {
    return AnswerOption(
      id: json['id'] as int,
      content: (json['content'] as String?) ?? '',
    );
  }
}
