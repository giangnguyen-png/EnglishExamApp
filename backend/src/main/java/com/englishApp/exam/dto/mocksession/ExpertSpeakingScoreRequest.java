package com.englishApp.exam.dto.mocksession;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record ExpertSpeakingScoreRequest(
		@NotNull BigDecimal score) {
}
