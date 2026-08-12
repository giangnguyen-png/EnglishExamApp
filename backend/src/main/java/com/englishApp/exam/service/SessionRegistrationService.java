package com.englishApp.exam.service;

import java.util.List;

import com.englishApp.exam.model.SessionRegistration;

public interface SessionRegistrationService {
	SessionRegistration registerSession(Integer sessionId, Integer userId);

	void cancelRegistration(Integer registrationId);

	List<SessionRegistration> findByUser(Integer userId);

	List<SessionRegistration> findBySession(Integer sessionId);
}
