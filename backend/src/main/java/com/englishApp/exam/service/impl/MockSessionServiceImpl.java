package com.englishApp.exam.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.model.enums.MockSessionStatus;
import com.englishApp.exam.repository.ExamRepository;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.SessionRegistrationRepository;
import com.englishApp.exam.repository.TestAttemptRepository;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.service.MockSessionService;
import com.englishApp.exam.service.TestAttemptService;

@Service
public class MockSessionServiceImpl implements MockSessionService {
	private final MockSessionRepository mockSessionRepository;
	private final UserRepository userRepository;
	private final ExamRepository examRepository;
	private final SessionRegistrationRepository sessionRegistrationRepository;
	private final TestAttemptRepository testAttemptRepository;
	private final TestAttemptService testAttemptService;

	public MockSessionServiceImpl(MockSessionRepository mockSessionRepository, UserRepository userRepository,
			ExamRepository examRepository, SessionRegistrationRepository sessionRegistrationRepository,
			TestAttemptRepository testAttemptRepository, TestAttemptService testAttemptService) {
		this.mockSessionRepository = mockSessionRepository;
		this.userRepository = userRepository;
		this.examRepository = examRepository;
		this.sessionRegistrationRepository = sessionRegistrationRepository;
		this.testAttemptRepository = testAttemptRepository;
		this.testAttemptService = testAttemptService;
	}

	public MockSession createSession(MockSession session) {
		validateSession(session);
		if (this.mockSessionRepository.existsByRoomCode(session.getRoomCode())) {
			throw new RuntimeException("Room code already exists");
		}
		if (hasTimeConflict(session, null)) {
			throw new RuntimeException("Session time is duplicated");
		}
		if (session.getStatus() == null) {
			session.setStatus(MockSessionStatus.PENDING);
		}
		return this.mockSessionRepository.save(session);
	}

	public MockSession updateSession(Integer id, MockSession updatedSession) {
		MockSession session = this.findById(id);
		if (session.getStatus() != MockSessionStatus.PENDING) {
			throw new RuntimeException("Only pending sessions can be updated");
		}
		validateSession(updatedSession);
		if (!session.getRoomCode().equals(updatedSession.getRoomCode())
				&& this.mockSessionRepository.existsByRoomCode(updatedSession.getRoomCode())) {
			throw new RuntimeException("Room code already exists");
		}
		if (hasTimeConflict(updatedSession, id)) {
			throw new RuntimeException("Session time is duplicated");
		}
		session.setRoomCode(updatedSession.getRoomCode());
		session.setStartTime(updatedSession.getStartTime());
		session.setEndTime(updatedSession.getEndTime());
		session.setRegistrationDeadline(updatedSession.getRegistrationDeadline());
		session.setMaxCandidates(updatedSession.getMaxCandidates());
		session.setStatus(updatedSession.getStatus());
		session.setExpert(updatedSession.getExpert());
		session.setExam(updatedSession.getExam());
		return this.mockSessionRepository.save(session);
	}

	public void deleteSession(Integer id) {
		MockSession session = this.findById(id);
		if (session.getStatus() != MockSessionStatus.PENDING) {
			throw new RuntimeException("Chỉ có thể xóa ca thi đang chờ bắt đầu.");
		}
		if (hasSessionData(id)) {
			throw new RuntimeException("Không thể xóa ca thi đã có dữ liệu thí sinh.");
		}
		this.mockSessionRepository.delete(session);
	}

	public void deleteSessionByAdmin(Integer id) {
		MockSession session = this.findById(id);
		if (hasSessionData(id)) {
			throw new RuntimeException("Không thể xóa ca thi đã có dữ liệu thí sinh.");
		}
		this.mockSessionRepository.delete(session);
	}

	public MockSession findById(Integer id) {
		return this.mockSessionRepository.findById(id).orElseThrow(() -> new RuntimeException("Session not found"));
	}

	public List<MockSession> findAll() {
		return this.mockSessionRepository.findAll();
	}

	public List<MockSession> findByExpert(Integer expertId) {
		if (!this.userRepository.existsById(expertId)) {
			throw new RuntimeException("Expert not found");
		}
		return this.mockSessionRepository.findByExpertId(expertId);
	}

	public List<MockSession> findAvailableSessions() {
		LocalDateTime now = LocalDateTime.now();
		return this.mockSessionRepository.findByStatus(MockSessionStatus.PENDING).stream().filter(
				session -> session.getRegistrationDeadline() == null || session.getRegistrationDeadline().isAfter(now))
				.toList();
	}

	public MockSession startSession(Integer id) {
		MockSession session = this.findById(id);
		if (session.getStatus() != MockSessionStatus.PENDING) {
			throw new RuntimeException("Only pending sessions can be started");
		}
		session.setStatus(MockSessionStatus.ONGOING);
		return this.mockSessionRepository.save(session);
	}

	public MockSession finishSession(Integer id) {
		MockSession session = this.findById(id);
		if (session.getStatus() != MockSessionStatus.ONGOING) {
			throw new RuntimeException("Only ongoing sessions can be finished");
		}
		session.setStatus(MockSessionStatus.COMPLETED);
		MockSession savedSession = this.mockSessionRepository.save(session);
		this.testAttemptRepository.findBySessionIdAndEndTimeIsNull(id)
				.forEach(attempt -> this.testAttemptService.forceSubmitAttempt(attempt.getId()));
		return savedSession;
	}

	private void validateSession(MockSession session) {
		if (session == null || session.getRoomCode() == null || session.getRoomCode().isBlank()) {
			throw new RuntimeException("Room code is required");
		}
		if (session.getStartTime() == null || session.getEndTime() == null) {
			throw new RuntimeException("Session time is required");
		}
		if (!session.getStartTime().isBefore(session.getEndTime())) {
			throw new RuntimeException("Session time is invalid");
		}
		if (session.getRegistrationDeadline() != null
				&& !session.getRegistrationDeadline().isBefore(session.getStartTime())) {
			throw new RuntimeException("Registration deadline must be before start time");
		}
		if (session.getMaxCandidates() <= 0) {
			throw new RuntimeException("Max candidates must be greater than 0");
		}
		if (session.getExpert() == null || session.getExpert().getId() == null
				|| !this.userRepository.existsById(session.getExpert().getId())) {
			throw new RuntimeException("Expert not found");
		}
		if (session.getExam() == null || session.getExam().getId() == null
				|| !this.examRepository.existsById(session.getExam().getId())) {
			throw new RuntimeException("Exam not found");
		}
	}

	private boolean hasTimeConflict(MockSession session, Integer currentSessionId) {
		return this.mockSessionRepository.findAll().stream()
				.filter(existing -> currentSessionId == null || !existing.getId().equals(currentSessionId))
				.filter(existing -> existing.getRoomCode().equals(session.getRoomCode())
						|| existing.getExpert().getId().equals(session.getExpert().getId()))
				.anyMatch(existing -> session.getStartTime().isBefore(existing.getEndTime())
						&& session.getEndTime().isAfter(existing.getStartTime()));
	}

	private boolean hasSessionData(Integer sessionId) {
		return this.sessionRegistrationRepository.countBySessionId(sessionId) > 0
				|| this.testAttemptRepository.countBySessionId(sessionId) > 0;
	}
}
