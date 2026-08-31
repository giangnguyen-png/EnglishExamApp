package com.englishApp.exam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.englishApp.exam.dto.attempt.AttemptReviewResponse;
import com.englishApp.exam.dto.attempt.FreeQuotaResponse;
import com.englishApp.exam.model.Answer;
import com.englishApp.exam.model.Exam;
import com.englishApp.exam.model.ExamSection;
import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.model.Question;
import com.englishApp.exam.model.SkillResult;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.User;
import com.englishApp.exam.model.UserResponse;
import com.englishApp.exam.model.UserResponseChoice;
import com.englishApp.exam.model.enums.MockSessionStatus;
import com.englishApp.exam.model.enums.QuestionType;
import com.englishApp.exam.model.enums.SkillType;
import com.englishApp.exam.repository.AnswerRepository;
import com.englishApp.exam.repository.ExamRepository;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.QuestionRepository;
import com.englishApp.exam.repository.SessionRegistrationRepository;
import com.englishApp.exam.repository.SkillResultRepository;
import com.englishApp.exam.repository.TestAttemptRepository;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.repository.UserResponseRepository;
import com.englishApp.exam.service.AiService;
import com.englishApp.exam.service.PaymentService;
import com.englishApp.exam.service.ScoringService;

@ExtendWith(MockitoExtension.class)
class TestAttemptServiceImplTest {
	@Mock
	private TestAttemptRepository testAttemptRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private ExamRepository examRepository;
	@Mock
	private MockSessionRepository mockSessionRepository;
	@Mock
	private AnswerRepository answerRepository;
	@Mock
	private SessionRegistrationRepository sessionRegistrationRepository;
	@Mock
	private QuestionRepository questionRepository;
	@Mock
	private UserResponseRepository userResponseRepository;
	@Mock
	private SkillResultRepository skillResultRepository;
	@Mock
	private AiService aiService;
	@Mock
	private ScoringService scoringService;
	@Mock
	private PaymentService paymentService;

	private TestAttemptServiceImpl service;

	@BeforeEach
	void setUp() {
		this.service = new TestAttemptServiceImpl(this.testAttemptRepository, this.userRepository,
				this.examRepository, this.mockSessionRepository, this.answerRepository,
				this.sessionRegistrationRepository, this.questionRepository, this.userResponseRepository,
				this.skillResultRepository, this.aiService, this.scoringService, this.paymentService);
	}

	@Test
	void validateExamReadyRejectsObjectiveQuestionWithAnswersButNoCorrectAnswer() {
		Question question = new Question();
		question.setId(10);
		question.setQuestionType(QuestionType.SINGLE_CHOICE);
		Answer wrongAnswer = new Answer();
		wrongAnswer.setCorrect(false);

		when(this.questionRepository.findByExamSectionExamId(1)).thenReturn(List.of(question));
		when(this.answerRepository.findByQuestionId(10)).thenReturn(List.of(wrongAnswer));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.validateExamReadyForAttempt(1));

		assertEquals("Đề thi chưa được cấu hình đầy đủ.", exception.getMessage());
	}

	@Test
	void forceSubmitExpiredAttemptRejectsNormalPracticeAttempt() {
		TestAttempt attempt = new TestAttempt();
		attempt.setId(20);

		when(this.testAttemptRepository.findByIdForUpdate(20)).thenReturn(Optional.of(attempt));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.forceSubmitExpiredAttempt(20));

		assertEquals("Chức năng tự động nộp bài chỉ áp dụng cho ca thi thử Premium.", exception.getMessage());
	}

	@Test
	void forceSubmitExpiredAttemptRejectsUserForceSubmitBeforeSessionEndTime() {
		MockSession session = new MockSession();
		session.setStatus(MockSessionStatus.ONGOING);
		session.setEndTime(LocalDateTime.now().plusMinutes(10));
		TestAttempt attempt = new TestAttempt();
		attempt.setId(20);
		attempt.setSession(session);

		when(this.testAttemptRepository.findByIdForUpdate(20)).thenReturn(Optional.of(attempt));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.forceSubmitExpiredAttempt(20));

		assertEquals("Ca thi vẫn đang diễn ra.", exception.getMessage());
	}

	@Test
	void forceSubmitExpiredAttemptAllowsExpiredPremiumAttempt() {
		TestAttempt attempt = premiumAttempt(MockSessionStatus.ONGOING, LocalDateTime.now().minusMinutes(1));
		when(this.testAttemptRepository.findByIdForUpdate(20)).thenReturn(Optional.of(attempt));
		mockFinalizeDependencies();
		when(this.testAttemptRepository.save(attempt)).thenReturn(attempt);

		TestAttempt result = this.service.forceSubmitExpiredAttempt(20);

		assertSame(attempt, result);
		verify(this.testAttemptRepository).save(attempt);
	}

	@Test
	void forceSubmitExpiredAttemptAllowsAlreadyCompletedPremiumAttemptIdempotently() {
		TestAttempt attempt = premiumAttempt(MockSessionStatus.COMPLETED, LocalDateTime.now().minusMinutes(1));
		attempt.setEndTime(LocalDateTime.now().minusSeconds(10));
		when(this.testAttemptRepository.findByIdForUpdate(20)).thenReturn(Optional.of(attempt));

		TestAttempt result = this.service.forceSubmitExpiredAttempt(20);

		assertSame(attempt, result);
	}

	@Test
	void startAttemptRejectsFreeUserAfterFiveNormalAttempts() {
		User user = user(7);
		Exam exam = exam(1, List.of());
		when(this.userRepository.findById(7)).thenReturn(Optional.of(user));
		when(this.examRepository.findById(1)).thenReturn(Optional.of(exam));
		when(this.paymentService.hasPremiumAccess(7)).thenReturn(false);
		when(this.testAttemptRepository.countByUserIdAndSessionIsNull(7)).thenReturn(5L);

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> this.service.startAttempt(7, 1, null));

		assertEquals("403 FORBIDDEN \"Bạn đã sử dụng hết 5 lượt luyện tập miễn phí. Vui lòng nâng cấp Premium để tiếp tục.\"",
				exception.getMessage());
		verify(this.testAttemptRepository, never()).save(any(TestAttempt.class));
	}

	@Test
	void startAttemptAllowsFreeUserBeforeLimitAndCountsUnfinishedAttempts() {
		User user = user(7);
		Exam exam = exam(1, List.of());
		when(this.userRepository.findById(7)).thenReturn(Optional.of(user));
		when(this.examRepository.findById(1)).thenReturn(Optional.of(exam));
		when(this.paymentService.hasPremiumAccess(7)).thenReturn(false);
		when(this.testAttemptRepository.countByUserIdAndSessionIsNull(7)).thenReturn(4L);
		when(this.questionRepository.findByExamSectionExamId(1)).thenReturn(List.of());
		when(this.testAttemptRepository.save(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TestAttempt attempt = this.service.startAttempt(7, 1, null);

		assertSame(user, attempt.getUser());
		assertSame(exam, attempt.getExam());
		assertNull(attempt.getSession());
	}

	@Test
	void startAttemptAllowsPremiumUserUnlimitedNormalAttempts() {
		User user = user(7);
		Exam exam = exam(1, List.of());
		when(this.userRepository.findById(7)).thenReturn(Optional.of(user));
		when(this.examRepository.findById(1)).thenReturn(Optional.of(exam));
		when(this.paymentService.hasPremiumAccess(7)).thenReturn(true);
		when(this.questionRepository.findByExamSectionExamId(1)).thenReturn(List.of());
		when(this.testAttemptRepository.save(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TestAttempt attempt = this.service.startAttempt(7, 1, null);

		assertSame(user, attempt.getUser());
		verify(this.testAttemptRepository, never()).countByUserIdAndSessionIsNull(7);
	}

	@Test
	void startAttemptDoesNotApplyFreeQuotaToMockSessionAttempt() {
		User user = user(7);
		Exam exam = exam(1, List.of());
		MockSession session = new MockSession();
		session.setId(99);
		session.setExam(exam);
		session.setStatus(MockSessionStatus.ONGOING);
		session.setEndTime(LocalDateTime.now().plusMinutes(30));
		when(this.userRepository.findById(7)).thenReturn(Optional.of(user));
		when(this.examRepository.findById(1)).thenReturn(Optional.of(exam));
		when(this.questionRepository.findByExamSectionExamId(1)).thenReturn(List.of());
		when(this.mockSessionRepository.findById(99)).thenReturn(Optional.of(session));
		when(this.sessionRegistrationRepository.existsBySessionIdAndUserId(99, 7)).thenReturn(true);
		when(this.testAttemptRepository.existsByUserIdAndSessionId(7, 99)).thenReturn(false);
		when(this.testAttemptRepository.save(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TestAttempt attempt = this.service.startAttempt(7, 1, 99);

		assertSame(session, attempt.getSession());
		verify(this.paymentService, never()).hasPremiumAccess(7);
		verify(this.testAttemptRepository, never()).countByUserIdAndSessionIsNull(7);
	}

	@Test
	void getFreeQuotaReturnsRemainingForFreeUser() {
		when(this.userRepository.existsById(7)).thenReturn(true);
		when(this.paymentService.hasPremiumAccess(7)).thenReturn(false);
		when(this.testAttemptRepository.countByUserIdAndSessionIsNull(7)).thenReturn(4L);

		FreeQuotaResponse quota = this.service.getFreeQuota(7);

		assertFalse(quota.premium());
		assertEquals(5, quota.limit());
		assertEquals(4L, quota.used());
		assertEquals(1, quota.remaining());
	}

	@Test
	void getFreeQuotaReturnsNullRemainingForPremiumUser() {
		when(this.userRepository.existsById(7)).thenReturn(true);
		when(this.paymentService.hasPremiumAccess(7)).thenReturn(true);
		when(this.testAttemptRepository.countByUserIdAndSessionIsNull(7)).thenReturn(12L);

		FreeQuotaResponse quota = this.service.getFreeQuota(7);

		assertTrue(quota.premium());
		assertEquals(12L, quota.used());
		assertNull(quota.remaining());
	}

	@Test
	void getAttemptReviewRejectsOtherUserAttempt() {
		TestAttempt attempt = reviewAttempt(20, 7, true, List.of(listeningSection()));
		when(this.testAttemptRepository.findById(20)).thenReturn(Optional.of(attempt));

		assertThrows(ResponseStatusException.class, () -> this.service.getAttemptReview(20, 8));
	}

	@Test
	void getAttemptReviewRejectsUnsubmittedAttempt() {
		TestAttempt attempt = reviewAttempt(20, 7, false, List.of(listeningSection()));
		when(this.testAttemptRepository.findById(20)).thenReturn(Optional.of(attempt));

		assertThrows(ResponseStatusException.class, () -> this.service.getAttemptReview(20, 7));
	}

	@Test
	void getAttemptReviewReturnsExplanationAndExactMultipleChoiceCorrectnessAndUnanswered() {
		ExamSection listening = listeningSection();
		Question multi = question(1, QuestionType.MULTIPLE_CHOICE, listening);
		Answer correctA = answer(11, "A", true, "Vì A đúng", multi);
		Answer correctB = answer(12, "B", true, "Vì B đúng", multi);
		Answer wrong = answer(13, "C", false, "C không phù hợp", multi);
		multi.setAnswers(List.of(correctA, correctB, wrong));
		Question unanswered = question(2, QuestionType.SINGLE_CHOICE, listening);
		Answer onlyCorrect = answer(21, "D", true, "D đúng", unanswered);
		unanswered.setAnswers(List.of(onlyCorrect));
		listening.setQuestions(List.of(multi, unanswered));
		TestAttempt attempt = reviewAttempt(20, 7, true, List.of(listening));
		UserResponse response = response(100, attempt, multi, List.of(correctA, correctB));
		when(this.testAttemptRepository.findById(20)).thenReturn(Optional.of(attempt));
		when(this.userResponseRepository.findByAttemptId(20)).thenReturn(List.of(response));

		AttemptReviewResponse review = this.service.getAttemptReview(20, 7);

		assertEquals(1, review.sections().size());
		assertEquals(SkillType.LISTENING, review.sections().get(0).skillType());
		assertTrue(review.sections().get(0).questions().get(0).answered());
		assertTrue(review.sections().get(0).questions().get(0).correct());
		assertFalse(review.sections().get(0).questions().get(1).answered());
		assertFalse(review.sections().get(0).questions().get(1).correct());
		assertEquals("Vì A đúng", review.sections().get(0).questions().get(0).answers().get(0).explanation());
		assertTrue(review.sections().get(0).questions().get(0).answers().get(0).selected());
		assertFalse(review.sections().get(0).questions().get(0).answers().get(2).selected());
	}

	@Test
	void getAttemptReviewRequiresMultipleChoiceSelectedSetToMatchExactly() {
		ExamSection reading = readingSection();
		Question multi = question(1, QuestionType.MULTIPLE_CHOICE, reading);
		Answer correctA = answer(11, "A", true, null, multi);
		Answer correctB = answer(12, "B", true, null, multi);
		Answer wrong = answer(13, "C", false, null, multi);
		multi.setAnswers(List.of(correctA, correctB, wrong));
		reading.setQuestions(List.of(multi));
		TestAttempt attempt = reviewAttempt(20, 7, true, List.of(reading));
		UserResponse response = response(100, attempt, multi, List.of(correctA, wrong));
		when(this.testAttemptRepository.findById(20)).thenReturn(Optional.of(attempt));
		when(this.userResponseRepository.findByAttemptId(20)).thenReturn(List.of(response));

		AttemptReviewResponse review = this.service.getAttemptReview(20, 7);

		assertTrue(review.sections().get(0).questions().get(0).answered());
		assertFalse(review.sections().get(0).questions().get(0).correct());
	}

	private TestAttempt premiumAttempt(MockSessionStatus status, LocalDateTime endTime) {
		Exam exam = new Exam();
		exam.setId(1);
		MockSession session = new MockSession();
		session.setStatus(status);
		session.setEndTime(endTime);
		TestAttempt attempt = new TestAttempt();
		attempt.setId(20);
		attempt.setExam(exam);
		attempt.setSession(session);
		return attempt;
	}

	private User user(Integer id) {
		User user = new User();
		user.setId(id);
		return user;
	}

	private Exam exam(Integer id, List<ExamSection> sections) {
		Exam exam = new Exam();
		exam.setId(id);
		exam.setTitle("IELTS Practice");
		exam.setExamSections(sections);
		sections.forEach(section -> section.setExam(exam));
		return exam;
	}

	private TestAttempt reviewAttempt(Integer id, Integer userId, boolean submitted, List<ExamSection> sections) {
		TestAttempt attempt = new TestAttempt();
		attempt.setId(id);
		attempt.setUser(user(userId));
		attempt.setExam(exam(1, sections));
		if (submitted) {
			attempt.setEndTime(LocalDateTime.now());
		}
		return attempt;
	}

	private ExamSection listeningSection() {
		ExamSection section = new ExamSection();
		section.setId(1);
		section.setSkillType(SkillType.LISTENING);
		section.setSectionOrder(1);
		section.setMediaUrl("https://example.com/audio.mp3");
		section.setQuestions(List.of());
		return section;
	}

	private ExamSection readingSection() {
		ExamSection section = new ExamSection();
		section.setId(2);
		section.setSkillType(SkillType.READING);
		section.setSectionOrder(2);
		section.setPassageContent("Reading passage");
		section.setQuestions(List.of());
		return section;
	}

	private Question question(Integer id, QuestionType type, ExamSection section) {
		Question question = new Question();
		question.setId(id);
		question.setQuestionType(type);
		question.setContent("Question " + id);
		question.setOrderIndex(id);
		question.setExamSection(section);
		question.setAnswers(List.of());
		return question;
	}

	private Answer answer(Integer id, String content, boolean correct, String explanation, Question question) {
		Answer answer = new Answer();
		answer.setId(id);
		answer.setContent(content);
		answer.setCorrect(correct);
		answer.setExplanation(explanation);
		answer.setQuestion(question);
		return answer;
	}

	private UserResponse response(Integer id, TestAttempt attempt, Question question, List<Answer> selectedAnswers) {
		UserResponse response = new UserResponse();
		response.setId(id);
		response.setAttempt(attempt);
		response.setQuestion(question);
		List<UserResponseChoice> choices = selectedAnswers.stream().map(answer -> {
			UserResponseChoice choice = new UserResponseChoice();
			choice.setResponse(response);
			choice.setAnswer(answer);
			return choice;
		}).toList();
		response.setAnswers(choices);
		return response;
	}

	private void mockFinalizeDependencies() {
		when(this.scoringService.calculateObjectiveBand(20, SkillType.LISTENING)).thenReturn(BigDecimal.ZERO);
		when(this.scoringService.calculateObjectiveBand(20, SkillType.READING)).thenReturn(BigDecimal.ZERO);
		when(this.scoringService.calculateWritingBand(20)).thenReturn(BigDecimal.ZERO);
		when(this.scoringService.calculateOverallBand(20)).thenReturn(BigDecimal.ZERO);
		when(this.scoringService.getBandScoreMap(20)).thenReturn(Map.of());
		when(this.scoringService.hasCompleteSkillResults(Map.of())).thenReturn(false);
		when(this.questionRepository
				.findByExamSectionExamIdAndExamSectionSkillTypeOrderByExamSectionSectionOrderAscOrderIndexAsc(
						eq(1), any(SkillType.class)))
				.thenReturn(List.of());
		when(this.userResponseRepository.findByAttemptIdAndQuestionExamSectionSkillType(eq(20), any(SkillType.class)))
				.thenReturn(List.of());
		when(this.userResponseRepository.findByAttemptId(20)).thenReturn(List.of());
		when(this.skillResultRepository.findByAttemptIdAndSkillType(eq(20), any(SkillType.class)))
				.thenReturn(Optional.<SkillResult>empty());
	}
}
