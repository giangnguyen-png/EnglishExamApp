package com.englishApp.exam.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.englishApp.exam.model.TestAttempt;

import jakarta.persistence.LockModeType;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Integer> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select attempt from TestAttempt attempt where attempt.id = :id")
	Optional<TestAttempt> findByIdForUpdate(@Param("id") Integer id);

	List<TestAttempt> findByUserId(Integer userId);

	List<TestAttempt> findByExamId(Integer examId);

	List<TestAttempt> findBySessionId(Integer sessionId);

	List<TestAttempt> findBySessionIdAndEndTimeIsNull(Integer sessionId);

	long countBySessionId(Integer sessionId);

	long countByUserIdAndSessionIsNull(Integer userId);

	List<TestAttempt> findByUserIdOrderByCreatedAtDesc(Integer userId);

	List<TestAttempt> findByUserIdAndEndTimeIsNotNullOrderByEndTimeDesc(Integer userId);

	boolean existsByUserIdAndSessionId(Integer userId, Integer sessionId);
}
