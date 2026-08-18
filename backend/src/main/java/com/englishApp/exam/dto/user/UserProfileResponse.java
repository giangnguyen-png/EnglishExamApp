package com.englishApp.exam.dto.user;

import com.englishApp.exam.model.User;

public record UserProfileResponse(
		Integer id,
		String username,
		String email,
		String fullName,
		String role) {
	public static UserProfileResponse from(User user) {
		return new UserProfileResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getFullName(),
				user.getRole().getName());
	}
}
