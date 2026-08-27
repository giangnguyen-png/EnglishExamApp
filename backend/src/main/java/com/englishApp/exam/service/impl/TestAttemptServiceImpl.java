package com.englishApp.exam.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.englishApp.exam.dto.ai.AiFeedback;
import com.englishApp.exam.dto.ai.AiEvaluationResult;
import com.englishApp.exam.model.Answer;
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
import com.englishApp.exam.repository.AnswerRepository;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.QuestionRepository;
import com.englishApp.exam.repository.SessionRegistrationRepository;
import com.englishApp.exam.repository.SkillResultRepository;
import com.englishApp.exam.repository.TestAttemptRepository;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.repository.UserResponseRepository;
import com.englishApp.exam.service.AiService;
import com.englishApp.exam.service.QuestionAnswerRules;
import com.englishApp.exam.service.ScoringService;
import com.englishApp.exam.service.TestAttemptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TestAttemptServiceImpl implements TestAttemptService {
	private final TestAttemptRepository testAttemptRepository;
	private final UserRepository userRepository;
	private final ExamRepository examRepository;
	private final MockSessionRepository mockSessionRepository;
	private final AnswerRepository answerRepository;
	private final SessionRegistrationRepository sessionRegistrationRepository;
	private final QuestionRepository questionRepository;
	private final UserResponseRepository userResponseRepository;
	private final SkillResultRepository skillResultRepository;
	private final AiService aiService;
	private final ScoringService scoringService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public TestAttemptServiceImpl(TestAttemptRepository testAttemptRepository, UserRepository userRepository,
			ExamRepository examRepository, MockSessionRepository mockSessionRepository,
			AnswerRepository answerRepository,
			SessionRegistrationRepository sessionRegistrationRepository, QuestionRepository questionRepository,
			UserResponseRepository userResponseRepository, SkillResultRepository skillResultRepository,
			AiService aiService, ScoringService scoringService) {
		this.testAttemptRepository = testAttemptRepository;
		this.userRepository = userRepository;
		this.examRepository = examRepository;
		this.mockSessionRepository = mockSessionRepository;
		this.answerRepository = answerRepository;
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

		if (exam.isPremiumOnly() && sessionId == null) {
			throw new RuntimeException("Premium exam must be taken through a mock session");
		}
		validateExamReadyForAttempt(examId);

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
		if (attempt.getSession() != null) {
			validatePremiumSpeakingComplete(attempt);
		}

		finalizeAttempt(attempt, false);
		return this.testAttemptRepository.save(attempt);
	}

	@Transactional
	public TestAttempt forceSubmitAttempt(Integer attemptId) {
		TestAttempt attempt = this.findById(attemptId);
		if (attempt.getEndTime() != null) {
			return attempt;
		}
		finalizeAttempt(attempt, true);
		return this.testAttemptRepository.save(attempt);
	}

	private void finalizeAttempt(TestAttempt attempt, boolean forced) {
		Integer attemptId = attempt.getId();
		attempt.setEndTime(LocalDateTime.now());
		saveSkillResult(attempt, SkillType.LISTENING,
				this.scoringService.calculateObjectiveBand(attemptId, SkillType.LISTENING), null);
		saveSkillResult(attempt, SkillType.READING,
				this.scoringService.calculateObjectiveBand(attemptId, SkillType.READING), null);
		finalizeWritingResponses(attempt);
		saveSkillResult(attempt, SkillType.WRITING, this.scoringService.calculateWritingBand(attemptId),
				buildAiAnalysis(attemptId, SkillType.WRITING));

		if (attempt.getSession() == null) {
			saveNormalSpeakingSkillResult(attempt, forced);
		} else {
			savePremiumSpeakingSkillResult(attempt, forced);
		}

		attempt.setOverallBandScore(this.scoringService.calculateOverallBand(attemptId));
		attempt.setAiOverallFeedback(buildOverallFeedbackJson(attemptId));
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

	public void validateExamReadyForAttempt(Integer examId) {
		List<Question> questions = this.questionRepository.findByExamSectionExamId(examId);
		for (Question question : questions) {
			if (!QuestionAnswerRules.usesChoiceAnswers(question.getQuestionType())) {
				continue;
			}
			List<Answer> answers = this.answerRepository.findByQuestionId(question.getId());
			long correctCount = answers.stream().filter(Answer::isCorrect).count();
			boolean validSingleCorrect = QuestionAnswerRules.isSingleCorrectChoice(question.getQuestionType())
					&& answers.size() >= 1 && correctCount == 1;
			boolean validMultipleCorrect = QuestionAnswerRules.isMultipleCorrectChoice(question.getQuestionType())
					&& answers.size() >= 1 && correctCount >= 1;
			if (!validSingleCorrect && !validMultipleCorrect) {
				throw new RuntimeException("Đề thi chưa được cấu hình đầy đủ.");
			}
		}
	}

	public List<TestAttempt> findByUser(Integer userId) {
		if (!this.userRepository.existsById(userId)) {
			throw new RuntimeException("User not found");
		}
		return this.testAttemptRepository.findByUserIdAndEndTimeIsNotNullOrderByEndTimeDesc(userId);
	}

	public List<TestAttempt> findBySession(Integer sessionId) {
		if (!this.mockSessionRepository.existsById(sessionId)) {
			throw new RuntimeException("Session not found");
		}
		return this.testAttemptRepository.findBySessionId(sessionId);
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

	private void saveWritingAnalysis(UserResponse response, AiEvaluationResult evaluation) {
		SkillResult result = this.skillResultRepository
				.findByAttemptIdAndSkillType(response.getAttempt().getId(), SkillType.WRITING)
				.orElseGet(SkillResult::new);
		result.setAttempt(response.getAttempt());
		result.setSkillType(SkillType.WRITING);
		List<Map<String, Object>> entries = readWritingAnalysis(result.getAiAnalysis());
		entries.removeIf(entry -> response.getId().equals(entry.get("responseId")));
		entries.add(Map.of("responseId", response.getId(), "questionId", response.getQuestion().getId(), "score",
				evaluation.score(), "feedback", evaluation.feedback()));
		result.setAiAnalysis(writeWritingAnalysis(entries));
		this.skillResultRepository.save(result);
	}

	private List<Map<String, Object>> readWritingAnalysis(String analysis) {
		if (analysis == null || analysis.isBlank()) {
			return new ArrayList<>();
		}
		try {
			return new ArrayList<>(
					this.objectMapper.readValue(analysis, new TypeReference<List<Map<String, Object>>>() {
					}));
		} catch (JsonProcessingException e) {
			return new ArrayList<>();
		}
	}

	private String writeWritingAnalysis(List<Map<String, Object>> entries) {
		try {
			return this.objectMapper.writeValueAsString(entries);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize AI analysis", e);
		}
	}

	private void saveNormalSpeakingSkillResult(TestAttempt attempt, boolean forced) {
		List<Question> speakingQuestions = this.questionRepository
				.findByExamSectionExamIdAndExamSectionSkillTypeOrderByExamSectionSectionOrderAscOrderIndexAsc(
						attempt.getExam().getId(), SkillType.SPEAKING);
		List<UserResponse> speakingResponses = this.userResponseRepository
				.findByAttemptIdAndQuestionExamSectionSkillType(attempt.getId(), SkillType.SPEAKING);
		List<UserResponse> validResponses = speakingResponses.stream().filter(this::hasValidSpeakingAudio).toList();
		if (validResponses.isEmpty()) {
			saveSkillResult(attempt, SkillType.SPEAKING, BigDecimal.ZERO.setScale(1), null);
			return;
		}
		AiEvaluationResult evaluation = this.aiService.evaluateSpeakingAttempt(buildSpeakingAttemptContent(attempt,
				validResponses));
		BigDecimal score = this.scoringService.roundToHalfBand(evaluation.score().doubleValue());
		if (forced && !speakingQuestions.isEmpty() && validResponses.size() < speakingQuestions.size()) {
			score = this.scoringService.roundToHalfBand(
					score.doubleValue() * validResponses.size() / speakingQuestions.size());
		}
		saveSkillResult(attempt, SkillType.SPEAKING, score,
				writeEvaluationAnalysis(evaluation));
	}

	private void savePremiumSpeakingSkillResult(TestAttempt attempt, boolean forced) {
		List<UserResponse> speakingResponses = this.userResponseRepository
				.findByAttemptIdAndQuestionExamSectionSkillType(attempt.getId(), SkillType.SPEAKING).stream()
				.filter(this::hasValidSpeakingAudio).toList();
		if (forced && speakingResponses.isEmpty()) {
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

	private void finalizeWritingResponses(TestAttempt attempt) {
		List<Question> writingQuestions = this.questionRepository
				.findByExamSectionExamIdAndExamSectionSkillTypeOrderByExamSectionSectionOrderAscOrderIndexAsc(
						attempt.getExam().getId(), SkillType.WRITING);
		Map<Integer, UserResponse> responseByQuestionId = this.userResponseRepository
				.findByAttemptIdAndQuestionExamSectionSkillType(attempt.getId(), SkillType.WRITING).stream()
				.collect(Collectors.toMap(response -> response.getQuestion().getId(), Function.identity()));
		for (int i = 0; i < writingQuestions.size(); i++) {
			Question question = writingQuestions.get(i);
			UserResponse response = responseByQuestionId.get(question.getId());
			if (response == null || response.getTextContent() == null || response.getTextContent().isBlank()) {
				continue;
			}
			if (response.getAiScore() != null) {
				continue;
			}
			AiEvaluationResult evaluation = this.aiService.evaluateWritingTask(question.getContent(),
					response.getTextContent(), i == 0 ? 1 : 2);
			BigDecimal normalizedScore = this.scoringService.roundToHalfBand(evaluation.score().doubleValue());
			response.setAiScore(normalizedScore);
			UserResponse savedResponse = this.userResponseRepository.save(response);
			saveWritingAnalysis(savedResponse, new AiEvaluationResult(normalizedScore, evaluation.feedback()));
		}
	}

	private void validatePremiumSpeakingComplete(TestAttempt attempt) {
		List<Question> speakingQuestions = this.questionRepository
				.findByExamSectionExamIdAndExamSectionSkillTypeOrderByExamSectionSectionOrderAscOrderIndexAsc(
						attempt.getExam().getId(), SkillType.SPEAKING);
		List<UserResponse> speakingResponses = this.userResponseRepository
				.findByAttemptIdAndQuestionExamSectionSkillType(attempt.getId(), SkillType.SPEAKING);
		Map<Integer, UserResponse> responseByQuestionId = speakingResponses.stream()
				.filter(this::hasValidSpeakingAudio)
				.collect(Collectors.toMap(response -> response.getQuestion().getId(), Function.identity(),
						(existing, replacement) -> existing));

		boolean hasAnsweredAllQuestions = speakingQuestions.stream()
				.allMatch(question -> responseByQuestionId.containsKey(question.getId()));
		if (!hasAnsweredAllQuestions) {
			throw new RuntimeException("Please answer all Speaking questions before submitting");
		}
	}

	private boolean hasValidSpeakingAudio(UserResponse response) {
		return (response.getFileUrl() != null && !response.getFileUrl().isBlank())
				|| (response.getFilePublicId() != null && !response.getFilePublicId().isBlank());
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
		if (session.getEndTime() != null && !LocalDateTime.now().isBefore(session.getEndTime())) {
			throw new RuntimeException("Ca thi đã kết thúc.");
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
