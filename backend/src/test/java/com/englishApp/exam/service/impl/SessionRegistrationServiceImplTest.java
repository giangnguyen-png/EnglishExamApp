package com.englishApp.exam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.model.SessionRegistration;
import com.englishApp.exam.model.User;
import com.englishApp.exam.model.enums.MockSessionStatus;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.SessionRegistrationRepository;
import com.englishApp.exam.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SessionRegistrationServiceImplTest {
	@Mock
	private SessionRegistrationRepository sessionRegistrationRepository;
	@Mock
	private MockSessionRepository mockSessionRepository;
	@Mock
	private UserRepository userRepository;

	private SessionRegistrationServiceImpl service;

	@BeforeEach
	void setUp() {
		this.service = new SessionRegistrationServiceImpl(this.sessionRegistrationRepository,
				this.mockSessionRepository, this.userRepository);
	}

	@Test
	void registerSession_whenRegistrationIsValid_shouldAssignNextCandidateNumber() {
		MockSession session = session(MockSessionStatus.PENDING, LocalDateTime.now().plusDays(1), 5);
		User user = user(7);
		SessionRegistration existing = registration(session, user(2), 3);
		when(this.mockSessionRepository.findById(10)).thenReturn(Optional.of(session));
		when(this.userRepository.findById(7)).thenReturn(Optional.of(user));
		when(this.sessionRegistrationRepository.existsBySessionIdAndUserId(10, 7)).thenReturn(false);
		when(this.sessionRegistrationRepository.findBySessionId(10)).thenReturn(List.of(existing));
		when(this.sessionRegistrationRepository.save(any(SessionRegistration.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		SessionRegistration registration = this.service.registerSession(10, 7);

		assertSame(session, registration.getSession());
		assertSame(user, registration.getUser());
		assertEquals(4, registration.getCandidateNumber());
	}

	@Test
	void registerSession_whenUserAlreadyRegistered_shouldRejectDuplicateRegistration() {
		MockSession session = session(MockSessionStatus.PENDING, LocalDateTime.now().plusDays(1), 5);
		when(this.mockSessionRepository.findById(10)).thenReturn(Optional.of(session));
		when(this.userRepository.findById(7)).thenReturn(Optional.of(user(7)));
		when(this.sessionRegistrationRepository.existsBySessionIdAndUserId(10, 7)).thenReturn(true);

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.registerSession(10, 7));

		assertEquals("User already registered this session", exception.getMessage());
	}

	@Test
	void registerSession_whenDeadlinePassed_shouldRejectRegistration() {
		MockSession session = session(MockSessionStatus.PENDING, LocalDateTime.now().minusMinutes(1), 5);
		when(this.mockSessionRepository.findById(10)).thenReturn(Optional.of(session));
		when(this.userRepository.findById(7)).thenReturn(Optional.of(user(7)));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.registerSession(10, 7));

		assertEquals("Session registration deadline has passed", exception.getMessage());
	}

	@Test
	void registerSession_whenSessionIsNotPending_shouldRejectRegistration() {
		MockSession session = session(MockSessionStatus.ONGOING, LocalDateTime.now().plusDays(1), 5);
		when(this.mockSessionRepository.findById(10)).thenReturn(Optional.of(session));
		when(this.userRepository.findById(7)).thenReturn(Optional.of(user(7)));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.registerSession(10, 7));

		assertEquals("Session is not open for registration", exception.getMessage());
	}

	@Test
	void registerSession_whenSessionIsFull_shouldRejectRegistration() {
		MockSession session = session(MockSessionStatus.PENDING, LocalDateTime.now().plusDays(1), 1);
		when(this.mockSessionRepository.findById(10)).thenReturn(Optional.of(session));
		when(this.userRepository.findById(7)).thenReturn(Optional.of(user(7)));
		when(this.sessionRegistrationRepository.existsBySessionIdAndUserId(10, 7)).thenReturn(false);
		when(this.sessionRegistrationRepository.findBySessionId(10))
				.thenReturn(List.of(registration(session, user(2), 1)));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.registerSession(10, 7));

		assertEquals("Session is full", exception.getMessage());
	}

	@Test
	void cancelRegistration_whenSessionIsPending_shouldDeleteRegistration() {
		SessionRegistration registration = registration(
				session(MockSessionStatus.PENDING, LocalDateTime.now().plusDays(1), 5), user(7), 1);
		when(this.sessionRegistrationRepository.findById(20)).thenReturn(Optional.of(registration));

		this.service.cancelRegistration(20);

		verify(this.sessionRegistrationRepository).delete(registration);
	}

	@Test
	void cancelRegistration_whenSessionAlreadyStarted_shouldRejectCancellation() {
		SessionRegistration registration = registration(
				session(MockSessionStatus.ONGOING, LocalDateTime.now().plusDays(1), 5), user(7), 1);
		when(this.sessionRegistrationRepository.findById(20)).thenReturn(Optional.of(registration));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.cancelRegistration(20));

		assertEquals("Registration cannot be cancelled after the session starts", exception.getMessage());
	}

	private MockSession session(MockSessionStatus status, LocalDateTime deadline, int maxCandidates) {
		MockSession session = new MockSession();
		session.setId(10);
		session.setStatus(status);
		session.setRegistrationDeadline(deadline);
		session.setMaxCandidates(maxCandidates);
		return session;
	}

	private User user(Integer id) {
		User user = new User();
		user.setId(id);
		return user;
	}

	private SessionRegistration registration(MockSession session, User user, Integer candidateNumber) {
		SessionRegistration registration = new SessionRegistration();
		registration.setSession(session);
		registration.setUser(user);
		registration.setCandidateNumber(candidateNumber);
		return registration;
	}
}
