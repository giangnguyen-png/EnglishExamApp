package com.englishApp.exam.dto.ai;

import java.math.BigDecimal;

public record AiEvaluationResult(
		BigDecimal score,
		AiFeedback feedback) {
}
