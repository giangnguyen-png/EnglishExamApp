package com.englishApp.exam.dto.mocksession;

import java.util.List;

public record SpeakingAttemptResponse(
		Integer attemptId,
		Integer candidateNumber,
		String username,
		List<SpeakingResponseItem> responses) {
	public record SpeakingResponseItem(
			Integer questionId,
			String questionContent,
			String audioUrl,
			String transcript) {
	}
}
