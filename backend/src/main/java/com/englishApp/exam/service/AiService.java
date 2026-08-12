package com.englishApp.exam.service;

import com.englishApp.exam.dto.ai.AiEvaluationResult;

public interface AiService {
	AiEvaluationResult evaluateWriting(String question, String answer);

	AiEvaluationResult evaluateSpeaking(String question, String transcript);
}
