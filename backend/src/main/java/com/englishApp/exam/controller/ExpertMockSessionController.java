package com.englishApp.exam.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.englishApp.exam.dto.attempt.AttemptResultResponse;
import com.englishApp.exam.dto.mocksession.CreateMockSessionRequest;
import com.englishApp.exam.dto.mocksession.ExpertRegistrationResponse;
import com.englishApp.exam.dto.mocksession.ExpertSpeakingScoreRequest;
import com.englishApp.exam.dto.mocksession.MockSessionDetailResponse;
import com.englishApp.exam.dto.mocksession.MockSessionSummaryResponse;
import com.englishApp.exam.dto.mocksession.SpeakingAttemptResponse;
import com.englishApp.exam.dto.mocksession.SpeakingAttemptResponse.SpeakingResponseItem;
import com.englishApp.exam.dto.mocksession.UpdateMockSessionRequest;
import com.englishApp.exam.model.Exam;
import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.model.Question;
import com.englishApp.exam.model.SessionRegistration;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.User;
import com.englishApp.exam.model.UserResponse;
import com.englishApp.exam.service.ExamService;
import com.englishApp.exam.service.MockSessionService;
import com.englishApp.exam.service.SessionRegistrationService;
import com.englishApp.exam.service.TestAttemptService;
import com.englishApp.exam.service.UserResponseService;
import com.englishApp.exam.service.UserService;
import com.englishApp.exam.repository.QuestionRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/expert")
public class ExpertMockSessionController {
	private final MockSessionService mockSessionService;
	private final SessionRegistrationService sessionRegistrationService;
	private final TestAttemptService testAttemptService;
	private final UserResponseService userResponseService;
	private final UserService userService;
	private final ExamService examService;
	private final QuestionRepository questionRepository;

	public ExpertMockSessionController(MockSessionService mockSessionService,
			SessionRegistrationService sessionRegistrationService, TestAttemptService testAttemptService,
			UserResponseService userResponseService, UserService userService, ExamService examService,
			QuestionRepository questionRepository) {
		this.mockSessionService = mockSessionService;
		this.sessionRegistrationService = sessionRegistrationService;
		this.testAttemptService = testAttemptService;
		this.userResponseService = userResponseService;
		this.userService = userService;
		this.examService = examService;
		this.questionRepository = questionRepository;
	}

	@PostMapping("/mock-sessions")
	public ResponseEntity<MockSessionSummaryResponse> createSession(@Valid @RequestBody CreateMockSessionRequest request,
			Authentication authentication) {
		User currentExpert = getCurrentUser(authentication);
		MockSession session = buildSession(request, currentExpert);
		return ResponseEntity.ok(MockSessionSummaryResponse.from(this.mockSessionService.createSession(session)));
	}

	@GetMapping("/mock-sessions")
	public ResponseEntity<List<MockSessionSummaryResponse>> findMySessions(Authentication authentication) {
		User currentExpert = getCurrentUser(authentication);
		List<MockSessionSummaryResponse> sessions = this.mockSessionService.findByExpert(currentExpert.getId()).stream()
				.sorted(Comparator.comparing(MockSession::getStartTime))
				.map(MockSessionSummaryResponse::from)
				.toList();
		return ResponseEntity.ok(sessions);
	}

	@GetMapping("/mock-sessions/{sessionId}")
	public ResponseEntity<MockSessionDetailResponse> findSessionDetail(@PathVariable Integer sessionId,
			Authentication authentication) {
		User currentExpert = getCurrentUser(authentication);
		MockSession session = this.mockSessionService.findById(sessionId);
		validateSessionOwner(session, currentExpert);
		int registrationCount = this.sessionRegistrationService.findBySession(sessionId).size();
		return ResponseEntity.ok(MockSessionDetailResponse.from(session, registrationCount));
	}

	@PutMapping("/mock-sessions/{sessionId}")
	public ResponseEntity<MockSessionSummaryResponse> updateSession(@PathVariable Integer sessionId,
			@Valid @RequestBody UpdateMockSessionRequest request, Authentication authentication) {
		User currentExpert = getCurrentUser(authentication);
		MockSession currentSession = this.mockSessionService.findById(sessionId);
		validateSessionOwner(currentSession, currentExpert);
		MockSession updatedSession = buildSession(request, currentExpert);
		updatedSession.setStatus(currentSession.getStatus());
		return ResponseEntity.ok(MockSessionSummaryResponse.from(this.mockSessionService.updateSession(sessionId,
				updatedSession)));
	}

	@DeleteMapping("/mock-sessions/{sessionId}")
	public ResponseEntity<Void> deleteSession(@PathVariable Integer sessionId, Authentication authentication) {
		User currentExpert = getCurrentUser(authentication);
		MockSession session = this.mockSessionService.findById(sessionId);
		validateSessionOwner(session, currentExpert);
		this.mockSessionService.deleteSession(sessionId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/mock-sessions/{sessionId}/start")
	public ResponseEntity<MockSessionSummaryResponse> startSession(@PathVariable Integer sessionId,
			Authentication authentication) {
		User currentExpert = getCurrentUser(authentication);
		MockSession session = this.mockSessionService.findById(sessionId);
		validateSessionOwner(session, currentExpert);
		return ResponseEntity.ok(MockSessionSummaryResponse.from(this.mockSessionService.startSession(sessionId)));
	}

	@PostMapping("/mock-sessions/{sessionId}/finish")
	public ResponseEntity<MockSessionSummaryResponse> finishSession(@PathVariable Integer sessionId,
			Authentication authentication) {
		User currentExpert = getCurrentUser(authentication);
		MockSession session = this.mockSessionService.findById(sessionId);
		validateSessionOwner(session, currentExpert);
		return ResponseEntity.ok(MockSessionSummaryResponse.from(this.mockSessionService.finishSession(sessionId)));
	}

	@GetMapping("/mock-sessions/{sessionId}/registrations")
	public ResponseEntity<List<ExpertRegistrationResponse>> findSessionRegistrations(@PathVariable Integer sessionId,
			Authentication authentication) {
		User currentExpert = getCurrentUser(authentication);
		MockSession session = this.mockSessionService.findById(sessionId);
		validateSessionOwner(session, currentExpert);
		Map<Integer, TestAttempt> attemptByUserId = this.testAttemptService.findBySession(sessionId).stream()
				.collect(Collectors.toMap(attempt -> attempt.getUser().getId(), Function.identity(),
						(existing, replacement) -> existing));
		List<ExpertRegistrationResponse> registrations = this.sessionRegistrationService.findBySession(sessionId)
				.stream()
				.sorted(Comparator.comparing(SessionRegistration::getCandidateNumber))
				.map(registration -> ExpertRegistrationResponse.from(registration,
						attemptByUserId.get(registration.getUser().getId())))
				.toList();
		return ResponseEntity.ok(registrations);
	}

	@GetMapping("/mock-sessions/{sessionId}/speaking-attempts")
	public ResponseEntity<List<SpeakingAttemptResponse>> findSpeakingAttempts(@PathVariable Integer sessionId,
			Authentication authentication) {
		User currentExpert = getCurrentUser(authentication);
		MockSession session = this.mockSessionService.findById(sessionId);
		validateSessionOwner(session, currentExpert);
		return ResponseEntity.ok(toSpeakingAttempts(sessionId));
	}

	@PutMapping("/attempts/{attemptId}/speaking-score")
	public ResponseEntity<AttemptResultResponse> gradeSpeaking(@PathVariable Integer attemptId,
			@Valid @RequestBody ExpertSpeakingScoreRequest request, Authentication authentication) {
		User currentExpert = getCurrentUser(authentication);
		TestAttempt attempt = this.testAttemptService.gradeSpeakingAttempt(attemptId, currentExpert.getId(),
				request.score());
		return ResponseEntity.ok(AttemptResultResponse.from(attempt));
	}

	private MockSession buildSession(CreateMockSessionRequest request, User expert) {
		Exam exam = this.examService.findById(request.examId());
		MockSession session = new MockSession();
		session.setExam(exam);
		session.setExpert(expert);
		session.setRoomCode(request.roomCode());
		session.setStartTime(request.startTime());
		session.setEndTime(request.endTime());
		session.setRegistrationDeadline(request.registrationDeadline());
		session.setMaxCandidates(request.maxCandidates());
		return session;
	}

	private MockSession buildSession(UpdateMockSessionRequest request, User expert) {
		Exam exam = this.examService.findById(request.examId());
		MockSession session = new MockSession();
		session.setExam(exam);
		session.setExpert(expert);
		session.setRoomCode(request.roomCode());
		session.setStartTime(request.startTime());
		session.setEndTime(request.endTime());
		session.setRegistrationDeadline(request.registrationDeadline());
		session.setMaxCandidates(request.maxCandidates());
		return session;
	}

	private List<SpeakingAttemptResponse> toSpeakingAttempts(Integer sessionId) {
		MockSession session = this.mockSessionService.findById(sessionId);
		List<Question> speakingQuestions = this.questionRepository
				.findByExamSectionExamIdAndExamSectionSkillTypeOrderByExamSectionSectionOrderAscOrderIndexAsc(
						session.getExam().getId(), com.englishApp.exam.model.enums.SkillType.SPEAKING);
		Map<Integer, SessionRegistration> registrationByUserId = this.sessionRegistrationService.findBySession(sessionId)
				.stream()
				.collect(Collectors.toMap(registration -> registration.getUser().getId(), Function.identity()));
		Map<TestAttempt, List<UserResponse>> responsesByAttempt = this.userResponseService
				.findSpeakingResponsesBySession(sessionId).stream()
				.collect(Collectors.groupingBy(UserResponse::getAttempt, LinkedHashMap::new, Collectors.toList()));
		List<SpeakingAttemptResponse> attempts = new ArrayList<>();
		responsesByAttempt.forEach((attempt, responses) -> {
			SessionRegistration registration = registrationByUserId.get(attempt.getUser().getId());
			Integer candidateNumber = registration == null ? null : registration.getCandidateNumber();
			Map<Integer, UserResponse> responseByQuestionId = responses.stream()
					.collect(Collectors.toMap(response -> response.getQuestion().getId(), Function.identity(),
							(existing, replacement) -> existing));
			List<SpeakingResponseItem> responseItems = speakingQuestions.stream()
					.map(question -> {
						UserResponse response = responseByQuestionId.get(question.getId());
						if (response == null) {
							return new SpeakingResponseItem(question.getId(), question.getContent(), "", "Không trả lời");
						}
						return new SpeakingResponseItem(response.getQuestion().getId(), response.getQuestion().getContent(),
								response.getFileUrl(), response.getSpeechToTextTrans());
					})
					.toList();
			attempts.add(new SpeakingAttemptResponse(attempt.getId(), candidateNumber, attempt.getUser().getUsername(),
					responseItems));
		});
		attempts.sort(Comparator.comparing(SpeakingAttemptResponse::candidateNumber,
				Comparator.nullsLast(Comparator.naturalOrder())));
		return attempts;
	}

	private User getCurrentUser(Authentication authentication) {
		return this.userService.findByUsername(authentication.getName());
	}

	private void validateSessionOwner(MockSession session, User currentExpert) {
		if (!session.getExpert().getId().equals(currentExpert.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session does not belong to current expert");
		}
	}
}
