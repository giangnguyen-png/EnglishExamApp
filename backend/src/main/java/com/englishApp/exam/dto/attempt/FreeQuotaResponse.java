package com.englishApp.exam.dto.attempt;

public record FreeQuotaResponse(
		boolean premium,
		int limit,
		long used,
		Integer remaining) {
}
