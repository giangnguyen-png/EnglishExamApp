class Attempt {
  final int attemptId;
  final int examId;
  final int? sessionId;
  final String startTime;
  final String status;

  Attempt({
    required this.attemptId,
    required this.examId,
    this.sessionId,
    required this.startTime,
    required this.status,
  });

  factory Attempt.fromJson(Map<String, dynamic> json) {
    return Attempt(
      attemptId: json['attemptId'] as int,
      examId: json['examId'] as int,
      sessionId: json['sessionId'] as int?,
      startTime: (json['startTime'] as String?) ?? '',
      status: (json['status'] as String?) ?? '',
    );
  }
}
