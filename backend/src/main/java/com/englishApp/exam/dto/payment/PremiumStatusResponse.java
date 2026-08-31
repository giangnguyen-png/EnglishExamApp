package com.englishApp.exam.dto.payment;

import java.time.LocalDateTime;

public record PremiumStatusResponse(
		boolean premium,
		LocalDateTime expiresAt,
		String message) {
}
