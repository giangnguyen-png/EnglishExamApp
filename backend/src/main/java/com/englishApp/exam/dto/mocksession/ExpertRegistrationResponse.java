package com.englishApp.exam.dto.mocksession;

import java.time.LocalDateTime;

import com.englishApp.exam.model.SessionRegistration;

public record ExpertRegistrationResponse(
		Integer registrationId,
		Integer candidateNumber,
		Integer userId,
		String username,
		LocalDateTime registeredAt) {
	public static ExpertRegistrationResponse from(SessionRegistration registration) {
		return new ExpertRegistrationResponse(
				registration.getId(),
				registration.getCandidateNumber(),
				registration.getUser().getId(),
				registration.getUser().getUsername(),
				registration.getRegisteredAt());
	}
}
