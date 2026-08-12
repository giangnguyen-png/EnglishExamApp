package com.englishApp.exam.dto.ai;

import java.util.List;

public record AiFeedback(
		List<String> strengths,
		List<String> weaknesses,
		List<String> improvements) {
}
