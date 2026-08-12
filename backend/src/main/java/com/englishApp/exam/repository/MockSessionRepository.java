package com.englishApp.exam.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.model.enums.MockSessionStatus;

public interface MockSessionRepository extends JpaRepository<MockSession, Integer> {
	Optional<MockSession> findByRoomCode(String roomCode);

	boolean existsByRoomCode(String roomCode);

	List<MockSession> findByStatus(MockSessionStatus status);

	List<MockSession> findByExpertId(Integer id);
}
