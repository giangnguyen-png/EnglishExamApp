package com.englishApp.exam.dto.attempt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.englishApp.exam.model.TestAttempt;

public record AttemptHistoryResponse(
		Integer attemptId,
		Integer examId,
		String examTitle,
		BigDecimal overallBandScore,
		LocalDateTime startTime,
		LocalDateTime endTime) {
	public static AttemptHistoryResponse from(TestAttempt attempt) {
		return new AttemptHistoryResponse(
				attempt.getId(),
				attempt.getExam().getId(),
				attempt.getExam().getTitle(),
				attempt.getOverallBandScore(),
				attempt.getStartTime(),
				attempt.getEndTime());
	}
}
