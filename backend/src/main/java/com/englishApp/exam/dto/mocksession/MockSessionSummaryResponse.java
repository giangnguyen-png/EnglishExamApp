package com.englishApp.exam.dto.mocksession;

import java.time.LocalDateTime;

import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.model.enums.MockSessionStatus;

public record MockSessionSummaryResponse(
		Integer id,
		String roomCode,
		Integer examId,
		String examTitle,
		LocalDateTime startTime,
		LocalDateTime endTime,
		LocalDateTime registrationDeadline,
		int maxCandidates,
		MockSessionStatus status) {
	public static MockSessionSummaryResponse from(MockSession session) {
		return new MockSessionSummaryResponse(
				session.getId(),
				session.getRoomCode(),
				session.getExam().getId(),
				session.getExam().getTitle(),
				session.getStartTime(),
				session.getEndTime(),
				session.getRegistrationDeadline(),
				session.getMaxCandidates(),
				session.getStatus());
	}
}
