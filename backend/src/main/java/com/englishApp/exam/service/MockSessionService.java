package com.englishApp.exam.service;

import java.util.List;

import com.englishApp.exam.model.MockSession;

public interface MockSessionService {
	MockSession createSession(MockSession session);

	MockSession updateSession(Integer id, MockSession updatedSession);

	void deleteSession(Integer id);

	void deleteSessionByAdmin(Integer id);

	MockSession findById(Integer id);

	List<MockSession> findAll();

	List<MockSession> findByExpert(Integer expertId);

	List<MockSession> findAvailableSessions();

	MockSession startSession(Integer id);

	MockSession finishSession(Integer id);
}
