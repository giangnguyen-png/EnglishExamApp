package com.englishApp.exam.service;

import java.math.BigDecimal;
import java.util.List;

import com.englishApp.exam.dto.attempt.AttemptReviewResponse;
import com.englishApp.exam.dto.attempt.FreeQuotaResponse;
import com.englishApp.exam.model.TestAttempt;

public interface TestAttemptService {
	TestAttempt startAttempt(Integer userId, Integer examId, Integer sessionId);

	TestAttempt submitAttempt(Integer attemptId);

	TestAttempt forceSubmitAttempt(Integer attemptId);

	TestAttempt forceSubmitExpiredAttempt(Integer attemptId);

	TestAttempt gradeSpeakingAttempt(Integer attemptId, Integer expertId, BigDecimal score);

	TestAttempt findById(Integer id);

	void validateExamReadyForAttempt(Integer examId);

	List<TestAttempt> findByUser(Integer userId);

	List<TestAttempt> findBySession(Integer sessionId);

	BigDecimal calculateOverallBand(Integer attemptId);

	AttemptReviewResponse getAttemptReview(Integer attemptId, Integer userId);

	FreeQuotaResponse getFreeQuota(Integer userId);
}
