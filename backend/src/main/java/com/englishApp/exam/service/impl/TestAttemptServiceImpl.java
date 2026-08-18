package com.englishApp.exam.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.englishApp.exam.dto.ai.AiFeedback;
import com.englishApp.exam.dto.ai.AiEvaluationResult;
import com.englishApp.exam.model.Exam;
import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.model.Question;
import com.englishApp.exam.model.SkillResult;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.User;
import com.englishApp.exam.model.UserResponse;
import com.englishApp.exam.model.enums.MockSessionStatus;
import com.englishApp.exam.model.enums.SkillType;
import com.englishApp.exam.repository.ExamRepository;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.QuestionRepository;
import com.englishApp.exam.repository.SessionRegistrationRepository;
import com.englishApp.exam.repository.SkillResultRepository;
import com.englishApp.exam.repository.TestAttemptRepository;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.repository.UserResponseRepository;
import com.englishApp.exam.service.AiService;
import com.englishApp.exam.service.ScoringService;
import com.englishApp.exam.service.TestAttemptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TestAttemptServiceImpl implements TestAttemptService {
	private final TestAttemptRepository testAttemptRepository;
	private final UserRepository userRepository;
	private final ExamRepository examRepository;
	private final MockSessionRepository mockSessionRepository;
	private final SessionRegistrationRepository sessionRegistrationRepository;
	private final QuestionRepository questionRepository;
	private final UserResponseRepository userResponseRepository;
	private final SkillResultRepository skillResultRepository;
	private final AiService aiService;
	private final ScoringService scoringService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public TestAttemptServiceImpl(TestAttemptRepository testAttemptRepository, UserRepository userRepository,
			ExamRepository examRepository, MockSessionRepository mockSessionRepository,
			SessionRegistrationRepository sessionRegistrationRepository, QuestionRepository questionRepository,
			UserResponseRepository userResponseRepository, SkillResultRepository skillResultRepository,
			AiService aiService, ScoringService scoringService) {
		this.testAttemptRepository = testAttemptRepository;
		this.userRepository = userRepository;
		this.examRepository = examRepository;
		this.mockSessionRepository = mockSessionRepository;
		this.sessionRegistrationRepository = sessionRegistrationRepository;
		this.questionRepository = questionRepository;
		this.userResponseRepository = userResponseRepository;
		this.skillResultRepository = skillResultRepository;
		this.aiService = aiService;
		this.scoringService = scoringService;
	}

	public TestAttempt startAttempt(Integer userId, Integer examId, Integer sessionId) {
		User user = this.userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		Exam exam = this.examRepository.findById(examId).orElseThrow(() -> new RuntimeException("Exam not found"));

		TestAttempt attempt = new TestAttempt();
		attempt.setUser(user);
		attempt.setExam(exam);
		if (sessionId != null) {
			MockSession session = this.mockSessionRepository.findById(sessionId)
					.orElseThrow(() -> new RuntimeException("Session not found"));
			validatePremiumAttempt(userId, examId, sessionId, session);
			attempt.setSession(session);
		}
		attempt.setStartTime(LocalDateTime.now());
		return this.testAttemptRepository.save(attempt);
	}

	@Transactional
	public TestAttempt submitAttempt(Integer attemptId) {
		TestAttempt attempt = this.findById(attemptId);
		if (attempt.getEndTime() != null) {
			throw new RuntimeException("Test attempt has already been submitted");
		}
		attempt.setEndTime(LocalDateTime.now());
		saveSkillResult(attempt, SkillType.LISTENING,
				this.scoringService.calculateObjectiveBand(attemptId, SkillType.LISTENING), null);
		saveSkillResult(attempt, SkillType.READING,
				this.scoringService.calculateObjectiveBand(attemptId, SkillType.READING), null);
		saveSkillResult(attempt, SkillType.WRITING, this.scoringService.calculateWritingBand(attemptId),
				buildAiAnalysis(attemptId, SkillType.WRITING));

		if (attempt.getSession() == null) {
			saveNormalSpeakingSkillResult(attempt);
		} else {
			savePremiumSpeakingSkillResultOnSubmit(attempt);
		}

		attempt.setOverallBandScore(this.scoringService.calculateOverallBand(attemptId));
		attempt.setAiOverallFeedback(buildOverallFeedbackJson(attemptId));
		return this.testAttemptRepository.save(attempt);
	}

	@Transactional
	public TestAttempt gradeSpeakingAttempt(Integer attemptId, Integer expertId, BigDecimal score) {
		TestAttempt attempt = this.findById(attemptId);
		MockSession session = attempt.getSession();
		if (session == null) {
			throw new RuntimeException("Expert can only grade premium speaking attempts");
		}
		if (attempt.getEndTime() == null) {
			throw new RuntimeException("Test attempt has not been submitted yet");
		}
		List<UserResponse> speakingResponses = this.userResponseRepository
				.findByAttemptIdAndQuestionExamSectionSkillType(attemptId, SkillType.SPEAKING);
		if (speakingResponses.isEmpty()) {
			throw new RuntimeException("There are no speaking responses to grade");
		}
		User expert = this.userRepository.findById(expertId)
				.orElseThrow(() -> new RuntimeException("Expert not found"));
		if (session.getExpert() == null || !session.getExpert().getId().equals(expert.getId())) {
			throw new RuntimeException("Expert is not assigned to this session");
		}

		saveSkillResult(attempt, SkillType.SPEAKING, normalizeExpertScore(score), null);
		attempt.setOverallBandScore(this.scoringService.calculateOverallBand(attempt.getId()));
		attempt.setAiOverallFeedback(buildOverallFeedbackJson(attempt.getId()));
		return this.testAttemptRepository.save(attempt);
	}

	public TestAttempt findById(Integer id) {
		return this.testAttemptRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Test attempt not found"));
	}

	public List<TestAttempt> findByUser(Integer userId) {
		if (!this.userRepository.existsById(userId)) {
			throw new RuntimeException("User not found");
		}
		return this.testAttemptRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	public BigDecimal calculateOverallBand(Integer attemptId) {
		return this.scoringService.calculateOverallBand(attemptId);
	}

	private String buildOverallFeedbackJson(Integer attemptId) {
		Map<SkillType, BigDecimal> bandScores = this.scoringService.getBandScoreMap(attemptId);
		if (!this.scoringService.hasCompleteSkillResults(bandScores)) {
			return null;
		}
		String writingAiAnalysis = buildAiAnalysis(attemptId, SkillType.WRITING);
		String speakingAiAnalysis = buildAiAnalysis(attemptId, SkillType.SPEAKING);
		AiFeedback feedback = this.aiService.evaluateOverall(
				bandScores.get(SkillType.LISTENING),
				bandScores.get(SkillType.READING),
				bandScores.get(SkillType.WRITING),
				bandScores.get(SkillType.SPEAKING),
				writingAiAnalysis,
				speakingAiAnalysis);
		try {
			return this.objectMapper.writeValueAsString(feedback);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize overall AI feedback", e);
		}
	}

	private String buildAiAnalysis(Integer attemptId, SkillType skillType) {
		List<UserResponse> responses = this.userResponseRepository.findByAttemptId(attemptId).stream()
				.filter(response -> response.getQuestion().getExamSection().getSkillType() == skillType).toList();
		if (responses.isEmpty()) {
			return null;
		}

		return this.skillResultRepository.findByAttemptIdAndSkillType(attemptId, skillType)
				.map(SkillResult::getAiAnalysis).filter(analysis -> analysis != null && !analysis.isBlank())
				.orElse(null);
	}

	private SkillResult saveSkillResult(TestAttempt attempt, SkillType skillType, BigDecimal bandScore,
			String analysis) {
		SkillResult result = this.skillResultRepository.findByAttemptIdAndSkillType(attempt.getId(), skillType)
				.orElseGet(SkillResult::new);
		result.setAttempt(attempt);
		result.setSkillType(skillType);
		result.setBandScore(bandScore);
		result.setAiAnalysis(analysis);
		return this.skillResultRepository.save(result);
	}

	private void saveNormalSpeakingSkillResult(TestAttempt attempt) {
		List<UserResponse> speakingResponses = this.userResponseRepository
				.findByAttemptIdAndQuestionExamSectionSkillType(attempt.getId(), SkillType.SPEAKING);
		if (speakingResponses.isEmpty()) {
			saveSkillResult(attempt, SkillType.SPEAKING, BigDecimal.ZERO.setScale(1), null);
			return;
		}
		AiEvaluationResult evaluation = this.aiService.evaluateSpeakingAttempt(buildSpeakingAttemptContent(attempt,
				speakingResponses));
		saveSkillResult(attempt, SkillType.SPEAKING, this.scoringService.roundToHalfBand(evaluation.score().doubleValue()),
				writeEvaluationAnalysis(evaluation));
	}

	private void savePremiumSpeakingSkillResultOnSubmit(TestAttempt attempt) {
		List<UserResponse> speakingResponses = this.userResponseRepository
				.findByAttemptIdAndQuestionExamSectionSkillType(attempt.getId(), SkillType.SPEAKING);
		if (speakingResponses.isEmpty()) {
			saveSkillResult(attempt, SkillType.SPEAKING, BigDecimal.ZERO.setScale(1), null);
			return;
		}
		SkillResult result = this.skillResultRepository
				.findByAttemptIdAndSkillType(attempt.getId(), SkillType.SPEAKING)
				.orElseGet(SkillResult::new);
		if (result.getBandScore() != null) {
			return;
		}
		result.setAttempt(attempt);
		result.setSkillType(SkillType.SPEAKING);
		result.setBandScore(null);
		result.setAiAnalysis(null);
		this.skillResultRepository.save(result);
	}

	private String buildSpeakingAttemptContent(TestAttempt attempt, List<UserResponse> speakingResponses) {
		List<Question> speakingQuestions = this.questionRepository
				.findByExamSectionExamIdAndExamSectionSkillTypeOrderByExamSectionSectionOrderAscOrderIndexAsc(
						attempt.getExam().getId(), SkillType.SPEAKING);
		Map<Integer, UserResponse> responseByQuestionId = speakingResponses.stream()
				.collect(Collectors.toMap(response -> response.getQuestion().getId(), Function.identity()));
		StringBuilder content = new StringBuilder();
		content.append("Total speaking questions: ").append(speakingQuestions.size()).append('\n');
		content.append("Answered questions: ").append(speakingResponses.size()).append('\n');
		content.append("Unanswered questions should reduce the estimated overall performance when relevant.\n\n");

		if (speakingQuestions.isEmpty()) {
			speakingResponses.stream().sorted(speakingResponseComparator()).forEach(response -> appendSpeakingEntry(content,
					response.getQuestion(), response));
			return content.toString();
		}

		for (Question question : speakingQuestions) {
			appendSpeakingEntry(content, question, responseByQuestionId.get(question.getId()));
		}
		return content.toString();
	}

	private void appendSpeakingEntry(StringBuilder content, Question question, UserResponse response) {
		content.append("Question ").append(question.getOrderIndex()).append(":\n");
		content.append(question.getContent()).append('\n');
		content.append("Candidate response:\n");
		if (response == null || response.getSpeechToTextTrans() == null || response.getSpeechToTextTrans().isBlank()) {
			content.append("[NO RESPONSE]\n\n");
		} else {
			content.append(response.getSpeechToTextTrans()).append("\n\n");
		}
	}

	private Comparator<UserResponse> speakingResponseComparator() {
		return Comparator
				.comparing((UserResponse response) -> response.getQuestion().getExamSection().getSectionOrder())
				.thenComparing(response -> response.getQuestion().getOrderIndex());
	}

	private void validatePremiumAttempt(Integer userId, Integer examId, Integer sessionId, MockSession session) {
		if (!session.getExam().getId().equals(examId)) {
			throw new RuntimeException("Session does not belong to this exam");
		}
		if (!this.sessionRegistrationRepository.existsBySessionIdAndUserId(sessionId, userId)) {
			throw new RuntimeException("User is not registered for this session");
		}
		if (session.getStatus() != MockSessionStatus.ONGOING) {
			throw new RuntimeException("Session has not started");
		}
		if (this.testAttemptRepository.existsByUserIdAndSessionId(userId, sessionId)) {
			throw new RuntimeException("User already has an attempt for this session");
		}
	}

	private String writeEvaluationAnalysis(AiEvaluationResult evaluation) {
		try {
			return this.objectMapper.writeValueAsString(Map.of("score", evaluation.score(), "feedback",
					evaluation.feedback()));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize AI analysis", e);
		}
	}

	private BigDecimal normalizeExpertScore(BigDecimal score) {
		if (score == null) {
			throw new RuntimeException("Score is required");
		}
		if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.valueOf(9.0)) > 0) {
			throw new RuntimeException("Score must be between 0.0 and 9.0");
		}
		return this.scoringService.roundToHalfBand(score.doubleValue());
	}
}
