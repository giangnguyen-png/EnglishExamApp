package com.englishApp.exam.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.englishApp.exam.model.SessionRegistration;

public interface SessionRegistrationRepository extends JpaRepository<SessionRegistration, Integer> {
	List<SessionRegistration> findByUserId(Integer userId);

	List<SessionRegistration> findBySessionId(Integer sessionId);

	boolean existsBySessionIdAndUserId(Integer sessionId, Integer userId);
}
