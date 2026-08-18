package com.englishApp.exam.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
		@NotBlank @Email String email,
		@NotBlank String fullName) {
}
