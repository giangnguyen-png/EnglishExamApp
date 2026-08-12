package com.englishApp.exam.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.englishApp.exam.dto.ai.AiEvaluationResult;
import com.englishApp.exam.model.Answer;
import com.englishApp.exam.model.Exam;
import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.model.Question;
import com.englishApp.exam.model.SkillResult;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.User;
import com.englishApp.exam.model.UserResponse;
import com.englishApp.exam.model.UserResponseChoice;
import com.englishApp.exam.model.enums.SkillType;
import com.englishApp.exam.repository.ExamRepository;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.SkillResultRepository;
import com.englishApp.exam.repository.TestAttemptRepository;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.repository.UserResponseChoiceRepository;
import com.englishApp.exam.repository.UserResponseRepository;
import com.englishApp.exam.service.AiService;
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
	private final UserResponseRepository userResponseRepository;
	private final UserResponseChoiceRepository userResponseChoiceRepository;
	private final SkillResultRepository skillResultRepository;
	private final AiService aiService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public TestAttemptServiceImpl(TestAttemptRepository testAttemptRepository, UserRepository userRepository,
			ExamRepository examRepository, MockSessionRepository mockSessionRepository,
			UserResponseRepository userResponseRepository, UserResponseChoiceRepository userResponseChoiceRepository,
			SkillResultRepository skillResultRepository, AiService aiService) {
		this.testAttemptRepository = testAttemptRepository;
		this.userRepository = userRepository;
		this.examRepository = examRepository;
		this.mockSessionRepository = mockSessionRepository;
		this.userResponseRepository = userResponseRepository;
		this.userResponseChoiceRepository = userResponseChoiceRepository;
		this.skillResultRepository = skillResultRepository;
		this.aiService = aiService;
	}

	public TestAttempt startAttempt(Integer userId, Integer examId, Integer sessionId) {
		User user = this.userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		Exam exam = this.examRepository.findById(examId).orElseThrow(() -> new RuntimeException("Exam not found"));
		MockSession session = this.mockSessionRepository.findById(sessionId)
				.orElseThrow(() -> new RuntimeException("Session not found"));
		TestAttempt attempt = new TestAttempt();
		attempt.setUser(user);
		attempt.setExam(exam);
		attempt.setSession(session);
		attempt.setStartTime(LocalDateTime.now());
		return this.testAttemptRepository.save(attempt);
	}

	public TestAttempt submitAttempt(Integer attemptId) {
		TestAttempt attempt = this.findById(attemptId);
		attempt.setEndTime(LocalDateTime.now());
		saveSkillResult(attempt, SkillType.LISTENING, calculateObjectiveBand(attemptId, SkillType.LISTENING), null);
		saveSkillResult(attempt, SkillType.READING, calculateObjectiveBand(attemptId, SkillType.READING), null);
		saveSkillResult(attempt, SkillType.WRITING, calculateAiBand(attemptId, SkillType.WRITING),
				buildAiAnalysis(attemptId, SkillType.WRITING));
		saveSkillResult(attempt, SkillType.SPEAKING, calculateAiBand(attemptId, SkillType.SPEAKING),
				buildAiAnalysis(attemptId, SkillType.SPEAKING));
		attempt.setOverallBandScore(this.calculateOverallBand(attemptId));
		attempt.setAiOverallFeedback(null);
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
		List<BigDecimal> bandScores = this.skillResultRepository.findByAttemptId(attemptId).stream()
				.map(SkillResult::getBandScore).filter(Objects::nonNull).toList();
		if (bandScores.isEmpty()) {
			return BigDecimal.ZERO.setScale(1);
		}
		double average = bandScores.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
		return roundToHalfBand(average);
	}

	private BigDecimal calculateObjectiveBand(Integer attemptId, SkillType skillType) {
		List<UserResponse> responses = this.userResponseRepository.findByAttemptId(attemptId).stream()
				.filter(response -> response.getQuestion().getExamSection().getSkillType() == skillType).toList();
		if (responses.isEmpty()) {
			return BigDecimal.ZERO.setScale(1);
		}
		long correctCount = responses.stream().filter(this::isCorrectResponse).count();
		return roundToHalfBand((correctCount * 9.0) / responses.size());
	}

	private BigDecimal calculateAiBand(Integer attemptId, SkillType skillType) {
		List<UserResponse> responses = this.userResponseRepository.findByAttemptId(attemptId).stream()
				.filter(response -> response.getQuestion().getExamSection().getSkillType() == skillType).toList();
		if (responses.isEmpty()) {
			return BigDecimal.ZERO.setScale(1);
		}
		double average = responses.stream().map(response -> ensureAiScore(response, skillType)).filter(Objects::nonNull)
				.mapToDouble(BigDecimal::doubleValue).average().orElse(0);
		return roundToHalfBand(average);
	}

	private BigDecimal ensureAiScore(UserResponse response, SkillType skillType) {
		if (response.getAiScore() != null) {
			return response.getAiScore();
		}
		if (skillType == SkillType.WRITING && response.getTextContent() != null) {
			AiEvaluationResult evaluation = this.aiService.evaluateWriting(response.getQuestion().getContent(),
					response.getTextContent());
			response.setAiScore(evaluation.score());
			this.userResponseRepository.save(response);
			saveAiAnalysis(response, skillType, evaluation);
		}
		if (skillType == SkillType.SPEAKING && response.getSpeechToTextTrans() != null) {
			AiEvaluationResult evaluation = this.aiService.evaluateSpeaking(response.getQuestion().getContent(),
					response.getSpeechToTextTrans());
			response.setAiScore(evaluation.score());
			this.userResponseRepository.save(response);
			saveAiAnalysis(response, skillType, evaluation);
		}
		return response.getAiScore();
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

	private boolean isCorrectResponse(UserResponse response) {
		Question question = response.getQuestion();
		List<Integer> correctAnswerIds = question.getAnswers().stream().filter(Answer::isCorrect).map(Answer::getId)
				.toList();
		List<Integer> selectedAnswerIds = this.userResponseChoiceRepository.findByResponseId(response.getId()).stream()
				.map(UserResponseChoice::getAnswer).map(Answer::getId).toList();
		if (correctAnswerIds.isEmpty() || selectedAnswerIds.isEmpty()) {
			return false;
		}
		Map<Integer, Long> selectedCounts = selectedAnswerIds.stream()
				.collect(Collectors.groupingBy(id -> id, Collectors.counting()));
		return selectedAnswerIds.size() == correctAnswerIds.size()
				&& correctAnswerIds.stream().allMatch(id -> selectedCounts.getOrDefault(id, 0L) == 1L);
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

	private void saveAiAnalysis(UserResponse response, SkillType skillType, AiEvaluationResult evaluation) {
		SkillResult result = this.skillResultRepository
				.findByAttemptIdAndSkillType(response.getAttempt().getId(), skillType).orElseGet(SkillResult::new);
		result.setAttempt(response.getAttempt());
		result.setSkillType(skillType);
		result.setAiAnalysis(writeAiAnalysis(mergeAiAnalysis(result.getAiAnalysis(), response, evaluation)));
		this.skillResultRepository.save(result);
	}

	private List<Map<String, Object>> mergeAiAnalysis(String currentAnalysis, UserResponse response,
			AiEvaluationResult evaluation) {
		List<Map<String, Object>> entries = readAiAnalysis(currentAnalysis);
		entries.removeIf(entry -> response.getId().equals(entry.get("responseId")));
		entries.add(Map.of("responseId", response.getId(), "questionId", response.getQuestion().getId(), "score",
				evaluation.score(), "feedback", evaluation.feedback()));
		return entries;
	}

	private List<Map<String, Object>> readAiAnalysis(String analysis) {
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

	private String writeAiAnalysis(List<Map<String, Object>> entries) {
		try {
			return this.objectMapper.writeValueAsString(entries);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize AI analysis", e);
		}
	}

	private BigDecimal roundToHalfBand(double value) {
		return BigDecimal.valueOf(Math.round(value * 2.0) / 2.0).setScale(1, RoundingMode.HALF_UP);
	}
}
