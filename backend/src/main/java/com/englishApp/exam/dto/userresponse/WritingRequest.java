package com.englishApp.exam.dto.userresponse;

import jakarta.validation.constraints.NotBlank;

public record WritingRequest(
		@NotBlank String textContent) {
}
