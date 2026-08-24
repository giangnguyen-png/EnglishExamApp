class MockSession {
  final int id;
  final String roomCode;
  final int examId;
  final String examTitle;
  final String startTime;
  final String endTime;
  final String registrationDeadline;
  final int maxCandidates;
  final String status;
  final int? registrationCount;

  MockSession({
    required this.id,
    required this.roomCode,
    required this.examId,
    required this.examTitle,
    required this.startTime,
    required this.endTime,
    required this.registrationDeadline,
    required this.maxCandidates,
    required this.status,
    this.registrationCount,
  });

  factory MockSession.fromJson(Map<String, dynamic> json) {
    return MockSession(
      id: json['id'] as int,
      roomCode: (json['roomCode'] as String?) ?? '',
      examId: json['examId'] as int,
      examTitle: (json['examTitle'] as String?) ?? '',
      startTime: (json['startTime'] as String?) ?? '',
      endTime: (json['endTime'] as String?) ?? '',
      registrationDeadline: (json['registrationDeadline'] as String?) ?? '',
      maxCandidates: (json['maxCandidates'] as int?) ?? 0,
      status: (json['status'] as String?) ?? '',
      registrationCount: json['registrationCount'] as int?,
    );
  }
}

class SessionRegistration {
  final int registrationId;
  final int sessionId;
  final int candidateNumber;
  final int? examId;
  final String roomCode;
  final String examTitle;
  final String startTime;
  final String endTime;
  final String status;

  SessionRegistration({
    required this.registrationId,
    required this.sessionId,
    required this.candidateNumber,
    this.examId,
    required this.roomCode,
    required this.examTitle,
    required this.startTime,
    required this.endTime,
    required this.status,
  });

  factory SessionRegistration.fromJson(Map<String, dynamic> json) {
    return SessionRegistration(
      registrationId: json['registrationId'] as int,
      sessionId: json['sessionId'] as int,
      candidateNumber: (json['candidateNumber'] as int?) ?? 0,
      examId: json['examId'] as int?,
      roomCode: (json['roomCode'] as String?) ?? '',
      examTitle: (json['examTitle'] as String?) ?? '',
      startTime: (json['startTime'] as String?) ?? '',
      endTime: (json['endTime'] as String?) ?? '',
      status: (json['status'] as String?) ?? '',
    );
  }
}

class ExpertRegistration {
  final int registrationId;
  final int candidateNumber;
  final int userId;
  final String username;
  final int? attemptId;
  final double? overallBandScore;
  final String registeredAt;

  ExpertRegistration({
    required this.registrationId,
    required this.candidateNumber,
    required this.userId,
    required this.username,
    this.attemptId,
    this.overallBandScore,
    required this.registeredAt,
  });

  factory ExpertRegistration.fromJson(Map<String, dynamic> json) {
    return ExpertRegistration(
      registrationId: json['registrationId'] as int,
      candidateNumber: (json['candidateNumber'] as int?) ?? 0,
      userId: json['userId'] as int,
      username: (json['username'] as String?) ?? '',
      attemptId: json['attemptId'] as int?,
      overallBandScore: _toDouble(json['overallBandScore']),
      registeredAt: (json['registeredAt'] as String?) ?? '',
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

class SpeakingAttempt {
  final int attemptId;
  final int candidateNumber;
  final String username;
  final List<SpeakingResponse> responses;

  SpeakingAttempt({
    required this.attemptId,
    required this.candidateNumber,
    required this.username,
    required this.responses,
  });

  factory SpeakingAttempt.fromJson(Map<String, dynamic> json) {
    final responsesJson = (json['responses'] as List<dynamic>?) ?? [];
    return SpeakingAttempt(
      attemptId: json['attemptId'] as int,
      candidateNumber: (json['candidateNumber'] as int?) ?? 0,
      username: (json['username'] as String?) ?? '',
      responses: responsesJson
          .map(
            (item) => SpeakingResponse.fromJson(item as Map<String, dynamic>),
          )
          .toList(),
    );
  }
}

class SpeakingResponse {
  final int questionId;
  final String questionContent;
  final String audioUrl;
  final String transcript;

  SpeakingResponse({
    required this.questionId,
    required this.questionContent,
    required this.audioUrl,
    required this.transcript,
  });

  factory SpeakingResponse.fromJson(Map<String, dynamic> json) {
    return SpeakingResponse(
      questionId: json['questionId'] as int,
      questionContent: (json['questionContent'] as String?) ?? '',
      audioUrl: (json['audioUrl'] as String?) ?? '',
      transcript: (json['transcript'] as String?) ?? '',
    );
  }
}
