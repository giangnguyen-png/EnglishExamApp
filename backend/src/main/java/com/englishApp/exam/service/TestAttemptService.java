package com.englishApp.exam.service;

import java.math.BigDecimal;
import java.util.List;

import com.englishApp.exam.model.TestAttempt;

public interface TestAttemptService {
	TestAttempt startAttempt(Integer userId, Integer examId, Integer sessionId);

	TestAttempt submitAttempt(Integer attemptId);

	TestAttempt gradeSpeakingAttempt(Integer attemptId, Integer expertId, BigDecimal score);

	TestAttempt findById(Integer id);

	List<TestAttempt> findByUser(Integer userId);

	BigDecimal calculateOverallBand(Integer attemptId);
}
