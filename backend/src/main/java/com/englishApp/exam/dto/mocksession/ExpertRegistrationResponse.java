package com.englishApp.exam.dto.mocksession;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.englishApp.exam.model.SessionRegistration;
import com.englishApp.exam.model.TestAttempt;

public record ExpertRegistrationResponse(
		Integer registrationId,
		Integer candidateNumber,
		Integer userId,
		String username,
		Integer attemptId,
		BigDecimal overallBandScore,
		LocalDateTime registeredAt) {
	public static ExpertRegistrationResponse from(SessionRegistration registration) {
		return from(registration, null);
	}

	public static ExpertRegistrationResponse from(SessionRegistration registration, TestAttempt attempt) {
		return new ExpertRegistrationResponse(
				registration.getId(),
				registration.getCandidateNumber(),
				registration.getUser().getId(),
				registration.getUser().getUsername(),
				attempt == null ? null : attempt.getId(),
				attempt == null ? null : attempt.getOverallBandScore(),
				registration.getRegisteredAt());
	}
}
