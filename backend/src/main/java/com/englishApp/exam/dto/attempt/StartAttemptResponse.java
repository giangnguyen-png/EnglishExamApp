package com.englishApp.exam.dto.attempt;

import java.time.LocalDateTime;

import com.englishApp.exam.model.TestAttempt;

public record StartAttemptResponse(
		Integer attemptId,
		Integer examId,
		LocalDateTime startTime,
		String status) {
	public static StartAttemptResponse from(TestAttempt attempt) {
		String status = attempt.getEndTime() == null ? "IN_PROGRESS" : "SUBMITTED";
		return new StartAttemptResponse(attempt.getId(), attempt.getExam().getId(), attempt.getStartTime(), status);
	}
}
