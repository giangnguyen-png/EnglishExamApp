package com.englishApp.exam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
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
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.enums.MockSessionStatus;
import com.englishApp.exam.repository.ExamRepository;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.SessionRegistrationRepository;
import com.englishApp.exam.repository.TestAttemptRepository;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.service.TestAttemptService;

@ExtendWith(MockitoExtension.class)
class MockSessionServiceImplTest {
	@Mock
	private MockSessionRepository mockSessionRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private ExamRepository examRepository;
	@Mock
	private SessionRegistrationRepository sessionRegistrationRepository;
	@Mock
	private TestAttemptRepository testAttemptRepository;
	@Mock
	private TestAttemptService testAttemptService;

	private MockSessionServiceImpl service;

	@BeforeEach
	void setUp() {
		this.service = new MockSessionServiceImpl(this.mockSessionRepository, this.userRepository,
				this.examRepository, this.sessionRegistrationRepository, this.testAttemptRepository,
				this.testAttemptService);
	}

	@Test
	void findByIdFinishesExpiredOngoingSessionAndForceSubmitsOpenAttempts() {
		MockSession session = new MockSession();
		session.setId(1);
		session.setStatus(MockSessionStatus.ONGOING);
		session.setEndTime(LocalDateTime.now().minusMinutes(1));
		TestAttempt openAttempt = new TestAttempt();
		openAttempt.setId(7);

		when(this.mockSessionRepository.findById(1)).thenReturn(Optional.of(session));
		when(this.mockSessionRepository.save(session)).thenReturn(session);
		when(this.testAttemptRepository.findBySessionIdAndEndTimeIsNull(1)).thenReturn(List.of(openAttempt));

		MockSession result = this.service.findById(1);

		assertEquals(MockSessionStatus.COMPLETED, result.getStatus());
		verify(this.testAttemptService).forceSubmitAttempt(7);
	}

	@Test
	void finishSessionIsIdempotentWhenSessionAlreadyCompleted() {
		MockSession session = new MockSession();
		session.setId(1);
		session.setStatus(MockSessionStatus.COMPLETED);

		when(this.mockSessionRepository.findById(1)).thenReturn(Optional.of(session));

		MockSession result = this.service.finishSession(1);

		assertEquals(MockSessionStatus.COMPLETED, result.getStatus());
		verify(this.testAttemptRepository, never()).findBySessionIdAndEndTimeIsNull(1);
	}
}
