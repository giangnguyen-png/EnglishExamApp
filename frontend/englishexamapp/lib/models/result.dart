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

  AttemptResult({
    required this.attemptId,
    required this.examId,
    required this.examTitle,
    required this.startTime,
    required this.endTime,
    required this.overallBandScore,
    required this.skills,
    required this.aiOverallFeedback,
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

  SkillResult({
    required this.skillType,
    required this.bandScore,
    required this.aiAnalysis,
  });

  factory SkillResult.fromJson(Map<String, dynamic> json) {
    return SkillResult(
      skillType: (json['skillType'] as String?) ?? '',
      bandScore: _toDouble(json['bandScore']),
      aiAnalysis: _readAiText(json['aiAnalysis']),
    );
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
