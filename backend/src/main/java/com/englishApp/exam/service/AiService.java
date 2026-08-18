package com.englishApp.exam.service;

import java.math.BigDecimal;

import com.englishApp.exam.dto.ai.AiFeedback;
import com.englishApp.exam.dto.ai.AiEvaluationResult;

public interface AiService {
	AiEvaluationResult evaluateWritingTask(String question, String answer, int taskNumber);

	AiEvaluationResult evaluateSpeakingAttempt(String speakingTestContent);

	AiFeedback evaluateOverall(BigDecimal listeningBand, BigDecimal readingBand, BigDecimal writingBand,
			BigDecimal speakingBand, String writingAiAnalysis, String speakingAiAnalysis);
}
