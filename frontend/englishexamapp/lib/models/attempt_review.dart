class FreeQuota {
  final bool premium;
  final int limit;
  final int used;
  final int? remaining;

  FreeQuota({
    required this.premium,
    required this.limit,
    required this.used,
    required this.remaining,
  });

  factory FreeQuota.fromJson(Map<String, dynamic> json) {
    return FreeQuota(
      premium: (json['premium'] as bool?) ?? false,
      limit: (json['limit'] as int?) ?? 5,
      used: (json['used'] as num?)?.toInt() ?? 0,
      remaining: (json['remaining'] as num?)?.toInt(),
    );
  }
}

class AttemptReview {
  final int attemptId;
  final String examTitle;
  final List<ReviewSection> sections;

  AttemptReview({
    required this.attemptId,
    required this.examTitle,
    required this.sections,
  });

  factory AttemptReview.fromJson(Map<String, dynamic> json) {
    final sectionsJson = (json['sections'] as List<dynamic>?) ?? [];
    return AttemptReview(
      attemptId: json['attemptId'] as int,
      examTitle: (json['examTitle'] as String?) ?? '',
      sections: sectionsJson
          .map((item) => ReviewSection.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}

class ReviewSection {
  final int id;
  final String skillType;
  final int sectionOrder;
  final String passageContent;
  final String mediaUrl;
  final List<ReviewQuestion> questions;

  ReviewSection({
    required this.id,
    required this.skillType,
    required this.sectionOrder,
    required this.passageContent,
    required this.mediaUrl,
    required this.questions,
  });

  factory ReviewSection.fromJson(Map<String, dynamic> json) {
    final questionsJson = (json['questions'] as List<dynamic>?) ?? [];
    return ReviewSection(
      id: json['id'] as int,
      skillType: (json['skillType'] as String?) ?? '',
      sectionOrder: (json['sectionOrder'] as int?) ?? 0,
      passageContent: (json['passageContent'] as String?) ?? '',
      mediaUrl: (json['mediaUrl'] as String?) ?? '',
      questions: questionsJson
          .map((item) => ReviewQuestion.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}

class ReviewQuestion {
  final int id;
  final String content;
  final String questionType;
  final int orderIndex;
  final String? imageUrl;
  final bool answered;
  final bool correct;
  final List<ReviewAnswer> answers;

  ReviewQuestion({
    required this.id,
    required this.content,
    required this.questionType,
    required this.orderIndex,
    this.imageUrl,
    required this.answered,
    required this.correct,
    required this.answers,
  });

  factory ReviewQuestion.fromJson(Map<String, dynamic> json) {
    final answersJson = (json['answers'] as List<dynamic>?) ?? [];
    return ReviewQuestion(
      id: json['id'] as int,
      content: (json['content'] as String?) ?? '',
      questionType: (json['questionType'] as String?) ?? '',
      orderIndex: (json['orderIndex'] as int?) ?? 0,
      imageUrl: _optionalString(json['imageUrl']),
      answered: (json['answered'] as bool?) ?? false,
      correct: (json['correct'] as bool?) ?? false,
      answers: answersJson
          .map((item) => ReviewAnswer.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}

class ReviewAnswer {
  final int id;
  final String content;
  final bool selected;
  final bool correct;
  final String explanation;

  ReviewAnswer({
    required this.id,
    required this.content,
    required this.selected,
    required this.correct,
    required this.explanation,
  });

  factory ReviewAnswer.fromJson(Map<String, dynamic> json) {
    return ReviewAnswer(
      id: json['id'] as int,
      content: (json['content'] as String?) ?? '',
      selected: (json['selected'] as bool?) ?? false,
      correct: (json['correct'] as bool?) ?? false,
      explanation: (json['explanation'] as String?) ?? '',
    );
  }
}

String? _optionalString(dynamic value) {
  final text = value?.toString().trim();
  return text == null || text.isEmpty ? null : text;
}
