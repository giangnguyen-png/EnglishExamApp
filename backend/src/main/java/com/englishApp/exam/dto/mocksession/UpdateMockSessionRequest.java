package com.englishApp.exam.dto.mocksession;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateMockSessionRequest(
		@NotNull Integer examId,
		@NotBlank String roomCode,
		@NotNull LocalDateTime startTime,
		@NotNull LocalDateTime endTime,
		LocalDateTime registrationDeadline,
		@Min(1) int maxCandidates) {
}
