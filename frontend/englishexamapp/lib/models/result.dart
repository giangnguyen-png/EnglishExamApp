import 'dart:convert';

class AttemptResult {
  final int attemptId;
  final int examId;
  final String examTitle;
  final String startTime;
  final String endTime;
  final double? overallBandScore;
  final List<SkillResult> skills;
  final String aiOverallFeedback;
  final AiFeedback overallFeedback;

  AttemptResult({
    required this.attemptId,
    required this.examId,
    required this.examTitle,
    required this.startTime,
    required this.endTime,
    required this.overallBandScore,
    required this.skills,
    required this.aiOverallFeedback,
    required this.overallFeedback,
  });

  factory AttemptResult.fromJson(Map<String, dynamic> json) {
    final skillsJson = (json['skills'] as List<dynamic>?) ?? [];

    return AttemptResult(
      attemptId: json['attemptId'] as int,
      examId: json['examId'] as int,
      examTitle: (json['examTitle'] as String?) ?? '',
      startTime: (json['startTime'] as String?) ?? '',
      endTime: (json['endTime'] as String?) ?? '',
      overallBandScore: _toDouble(json['overallBandScore']),
      skills: skillsJson
          .map((item) => SkillResult.fromJson(item as Map<String, dynamic>))
          .toList(),
      aiOverallFeedback: _readAiText(json['aiOverallFeedback']),
      overallFeedback: AiFeedback.fromJsonString(json['aiOverallFeedback']),
    );
  }

  SkillResult? skillByType(String skillType) {
    for (final skill in skills) {
      if (skill.skillType == skillType) {
        return skill;
      }
    }
    return null;
  }
}

class SkillResult {
  final String skillType;
  final double? bandScore;
  final String aiAnalysis;
  final AiFeedback feedback;
  final List<WritingTaskAnalysis> writingTasks;

  SkillResult({
    required this.skillType,
    required this.bandScore,
    required this.aiAnalysis,
    required this.feedback,
    required this.writingTasks,
  });

  factory SkillResult.fromJson(Map<String, dynamic> json) {
    final skillType = (json['skillType'] as String?) ?? '';
    final aiAnalysis = json['aiAnalysis'];
    return SkillResult(
      skillType: skillType,
      bandScore: _toDouble(json['bandScore']),
      aiAnalysis: _readAiText(aiAnalysis),
      feedback: AiFeedback.fromAnalysis(aiAnalysis),
      writingTasks: skillType == 'WRITING'
          ? WritingTaskAnalysis.listFromJsonString(aiAnalysis)
          : const [],
    );
  }
}

class AiFeedback {
  final List<String> strengths;
  final List<String> weaknesses;
  final List<String> improvements;

  const AiFeedback({
    this.strengths = const [],
    this.weaknesses = const [],
    this.improvements = const [],
  });

  bool get isEmpty =>
      strengths.isEmpty && weaknesses.isEmpty && improvements.isEmpty;

  factory AiFeedback.fromJsonString(dynamic value) {
    final decoded = _decodeJson(value);
    if (decoded is Map<String, dynamic>) {
      return AiFeedback.fromMap(decoded);
    }
    return const AiFeedback();
  }

  factory AiFeedback.fromAnalysis(dynamic value) {
    final decoded = _decodeJson(value);
    if (decoded is Map<String, dynamic>) {
      final feedback = decoded['feedback'];
      if (feedback is Map<String, dynamic>) {
        return AiFeedback.fromMap(feedback);
      }
      return AiFeedback.fromMap(decoded);
    }
    return const AiFeedback();
  }

  factory AiFeedback.fromMap(Map<String, dynamic> json) {
    return AiFeedback(
      strengths: _readStringList(json['strengths']),
      weaknesses: _readStringList(json['weaknesses']),
      improvements: _readStringList(json['improvements']),
    );
  }
}

class WritingTaskAnalysis {
  final int? questionId;
  final double? score;
  final AiFeedback feedback;

  const WritingTaskAnalysis({
    required this.questionId,
    required this.score,
    required this.feedback,
  });

  factory WritingTaskAnalysis.fromMap(Map<String, dynamic> json) {
    final feedback = json['feedback'];
    return WritingTaskAnalysis(
      questionId: _toInt(json['questionId']),
      score: _toDouble(json['score']),
      feedback: feedback is Map<String, dynamic>
          ? AiFeedback.fromMap(feedback)
          : const AiFeedback(),
    );
  }

  static List<WritingTaskAnalysis> listFromJsonString(dynamic value) {
    final decoded = _decodeJson(value);
    if (decoded is! List<dynamic>) {
      return const [];
    }
    return decoded
        .whereType<Map<String, dynamic>>()
        .map(WritingTaskAnalysis.fromMap)
        .toList();
  }
}

class AttemptHistory {
  final int attemptId;
  final int examId;
  final String examTitle;
  final double? overallBandScore;
  final String startTime;
  final String endTime;

  AttemptHistory({
    required this.attemptId,
    required this.examId,
    required this.examTitle,
    required this.overallBandScore,
    required this.startTime,
    required this.endTime,
  });

  factory AttemptHistory.fromJson(Map<String, dynamic> json) {
    return AttemptHistory(
      attemptId: json['attemptId'] as int,
      examId: json['examId'] as int,
      examTitle: (json['examTitle'] as String?) ?? '',
      overallBandScore: _toDouble(json['overallBandScore']),
      startTime: (json['startTime'] as String?) ?? '',
      endTime: (json['endTime'] as String?) ?? '',
    );
  }
}

double? _toDouble(dynamic value) {
  if (value == null) {
    return null;
  }
  if (value is num) {
    return value.toDouble();
  }
  return double.tryParse(value.toString());
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

dynamic _decodeJson(dynamic value) {
  if (value == null) {
    return null;
  }

  final text = value.toString().trim();
  if (!text.startsWith('{') && !text.startsWith('[')) {
    return null;
  }

  try {
    return jsonDecode(text);
  } catch (_) {
    return null;
  }
}

List<String> _readStringList(dynamic value) {
  if (value is! List<dynamic>) {
    return const [];
  }
  return value
      .map((item) => item?.toString().trim() ?? '')
      .where((item) => item.isNotEmpty)
      .toList();
}

String _readAiText(dynamic value) {
  if (value == null) {
    return '';
  }

  final text = value.toString();
  final trimmed = text.trim();
  if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) {
    return text;
  }

  try {
    final decoded = jsonDecode(trimmed);
    if (decoded is Map<String, dynamic>) {
      return decoded.entries
          .map((entry) => '${entry.key}: ${entry.value}')
          .join('\n');
    }
    if (decoded is List<dynamic>) {
      return decoded.map((item) => item.toString()).join('\n');
    }
  } catch (_) {
    return text;
  }

  return text;
}
