package com.englishApp.exam.dto.auth;

public record LoginResponse(
		String accessToken,
		String tokenType) {
}
