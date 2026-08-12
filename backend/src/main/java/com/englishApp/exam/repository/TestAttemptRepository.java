package com.englishApp.exam.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.englishApp.exam.model.TestAttempt;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Integer> {
	List<TestAttempt> findByUserId(Integer userId);

	List<TestAttempt> findByExamId(Integer examId);

	List<TestAttempt> findBySessionId(Integer sessionId);

	List<TestAttempt> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
