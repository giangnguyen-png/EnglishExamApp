package com.englishApp.exam.dto.mocksession;

import java.time.LocalDateTime;

import com.englishApp.exam.model.TestAttempt;

public record PremiumStartAttemptResponse(
		Integer attemptId,
		Integer examId,
		Integer sessionId,
		LocalDateTime startTime,
		String status) {
	public static PremiumStartAttemptResponse from(TestAttempt attempt) {
		String status = attempt.getEndTime() == null ? "IN_PROGRESS" : "SUBMITTED";
		return new PremiumStartAttemptResponse(
				attempt.getId(),
				attempt.getExam().getId(),
				attempt.getSession().getId(),
				attempt.getStartTime(),
				status);
	}
}
