package com.englishApp.exam.dto.mocksession;

import java.time.LocalDateTime;

import com.englishApp.exam.model.SessionRegistration;

public record MySessionRegistrationResponse(
		Integer registrationId,
		Integer candidateNumber,
		Integer sessionId,
		String roomCode,
		String examTitle,
		LocalDateTime startTime,
		LocalDateTime endTime) {
	public static MySessionRegistrationResponse from(SessionRegistration registration) {
		return new MySessionRegistrationResponse(
				registration.getId(),
				registration.getCandidateNumber(),
				registration.getSession().getId(),
				registration.getSession().getRoomCode(),
				registration.getSession().getExam().getTitle(),
				registration.getSession().getStartTime(),
				registration.getSession().getEndTime());
	}
}
