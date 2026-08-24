package com.englishApp.exam.dto.mocksession;

import java.time.LocalDateTime;

import com.englishApp.exam.model.SessionRegistration;
import com.englishApp.exam.model.enums.MockSessionStatus;

public record MySessionRegistrationResponse(
		Integer registrationId,
		Integer candidateNumber,
		Integer sessionId,
		String roomCode,
		Integer examId,
		String examTitle,
		LocalDateTime startTime,
		LocalDateTime endTime,
		MockSessionStatus status) {
	public static MySessionRegistrationResponse from(SessionRegistration registration) {
		return new MySessionRegistrationResponse(
				registration.getId(),
				registration.getCandidateNumber(),
				registration.getSession().getId(),
				registration.getSession().getRoomCode(),
				registration.getSession().getExam().getId(),
				registration.getSession().getExam().getTitle(),
				registration.getSession().getStartTime(),
				registration.getSession().getEndTime(),
				registration.getSession().getStatus());
	}
}
