package com.englishApp.exam.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.model.SessionRegistration;
import com.englishApp.exam.model.User;
import com.englishApp.exam.model.enums.MockSessionStatus;
import com.englishApp.exam.model.enums.RegisterStatus;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.SessionRegistrationRepository;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.service.SessionRegistrationService;

@Service
public class SessionRegistrationServiceImpl implements SessionRegistrationService {
	private final SessionRegistrationRepository sessionRegistrationRepository;
	private final MockSessionRepository mockSessionRepository;
	private final UserRepository userRepository;

	public SessionRegistrationServiceImpl(SessionRegistrationRepository sessionRegistrationRepository,
			MockSessionRepository mockSessionRepository, UserRepository userRepository) {
		this.sessionRegistrationRepository = sessionRegistrationRepository;
		this.mockSessionRepository = mockSessionRepository;
		this.userRepository = userRepository;
	}

	public SessionRegistration registerSession(Integer sessionId, Integer userId) {
		MockSession session = this.mockSessionRepository.findById(sessionId)
				.orElseThrow(() -> new RuntimeException("Session not found"));
		User user = this.userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		validateOpenRegistration(session);
		if (this.sessionRegistrationRepository.existsBySessionIdAndUserId(sessionId, userId)) {
			throw new RuntimeException("User already registered this session");
		}
		List<SessionRegistration> registrations = this.sessionRegistrationRepository.findBySessionId(sessionId);
		if (registrations.size() >= session.getMaxCandidates()) {
			throw new RuntimeException("Session is full");
		}
		SessionRegistration registration = new SessionRegistration();
		registration.setSession(session);
		registration.setUser(user);
		registration.setStatus(RegisterStatus.REGISTERED);
		registration.setCandidateNumber(nextCandidateNumber(registrations));
		return this.sessionRegistrationRepository.save(registration);
	}

	public void cancelRegistration(Integer registrationId) {
		SessionRegistration registration = this.sessionRegistrationRepository.findById(registrationId)
				.orElseThrow(() -> new RuntimeException("Session registration not found"));
		this.sessionRegistrationRepository.delete(registration);
	}

	public List<SessionRegistration> findByUser(Integer userId) {
		if (!this.userRepository.existsById(userId)) {
			throw new RuntimeException("User not found");
		}
		return this.sessionRegistrationRepository.findByUserId(userId);
	}

	public List<SessionRegistration> findBySession(Integer sessionId) {
		if (!this.mockSessionRepository.existsById(sessionId)) {
			throw new RuntimeException("Session not found");
		}
		return this.sessionRegistrationRepository.findBySessionId(sessionId);
	}

	private void validateOpenRegistration(MockSession session) {
		if (session.getStatus() != MockSessionStatus.PENDING) {
			throw new RuntimeException("Session is not open for registration");
		}
		if (session.getRegistrationDeadline() != null && session.getRegistrationDeadline().isBefore(LocalDateTime.now())) {
			throw new RuntimeException("Session registration deadline has passed");
		}
	}

	private Integer nextCandidateNumber(List<SessionRegistration> registrations) {
		return registrations.stream().map(SessionRegistration::getCandidateNumber).filter(number -> number != null)
				.max(Comparator.naturalOrder()).orElse(0) + 1;
	}
}
