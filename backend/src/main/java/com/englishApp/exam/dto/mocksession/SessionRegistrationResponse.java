package com.englishApp.exam.dto.mocksession;

import java.time.LocalDateTime;

import com.englishApp.exam.model.SessionRegistration;

public record SessionRegistrationResponse(
		Integer registrationId,
		Integer sessionId,
		Integer candidateNumber,
		LocalDateTime registeredAt) {
	public static SessionRegistrationResponse from(SessionRegistration registration) {
		return new SessionRegistrationResponse(
				registration.getId(),
				registration.getSession().getId(),
				registration.getCandidateNumber(),
				registration.getRegisteredAt());
	}
}
