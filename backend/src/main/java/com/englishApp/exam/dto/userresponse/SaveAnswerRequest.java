package com.englishApp.exam.dto.userresponse;

import java.util.List;

public record SaveAnswerRequest(
		List<Integer> answerIds) {
}
