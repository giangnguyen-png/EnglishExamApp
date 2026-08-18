package com.englishApp.exam.dto.userresponse;

import java.util.List;

public record SaveAnswerResponse(
		Integer attemptId,
		Integer questionId,
		List<Integer> answerIds,
		boolean saved) {
}
