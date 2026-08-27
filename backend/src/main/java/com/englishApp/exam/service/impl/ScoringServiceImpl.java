package com.englishApp.exam.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.englishApp.exam.model.Answer;
import com.englishApp.exam.model.Question;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.UserResponse;
import com.englishApp.exam.model.UserResponseChoice;
import com.englishApp.exam.model.enums.SkillType;
import com.englishApp.exam.repository.QuestionRepository;
import com.englishApp.exam.repository.SkillResultRepository;
import com.englishApp.exam.repository.TestAttemptRepository;
import com.englishApp.exam.repository.UserResponseChoiceRepository;
import com.englishApp.exam.repository.UserResponseRepository;
import com.englishApp.exam.service.ScoringService;

@Service
public class ScoringServiceImpl implements ScoringService {
	private final TestAttemptRepository testAttemptRepository;
	private final QuestionRepository questionRepository;
	private final UserResponseRepository userResponseRepository;
	private final UserResponseChoiceRepository userResponseChoiceRepository;
	private final SkillResultRepository skillResultRepository;

	public ScoringServiceImpl(TestAttemptRepository testAttemptRepository, QuestionRepository questionRepository,
			UserResponseRepository userResponseRepository, UserResponseChoiceRepository userResponseChoiceRepository,
			SkillResultRepository skillResultRepository) {
		this.testAttemptRepository = testAttemptRepository;
		this.questionRepository = questionRepository;
		this.userResponseRepository = userResponseRepository;
		this.userResponseChoiceRepository = userResponseChoiceRepository;
		this.skillResultRepository = skillResultRepository;
	}

	public BigDecimal calculateObjectiveBand(Integer attemptId, SkillType skillType) {
		if (skillType != SkillType.LISTENING && skillType != SkillType.READING) {
			throw new RuntimeException("Objective band can only be calculated for Listening or Reading");
		}
		TestAttempt attempt = findAttempt(attemptId);
		long totalQuestions = this.questionRepository
				.countByExamSectionExamIdAndExamSectionSkillType(attempt.getExam().getId(), skillType);
		if (totalQuestions != 40) {
			throw new RuntimeException("Listening and Reading exams must contain exactly 40 questions");
		}
		List<UserResponse> responses = this.userResponseRepository.findByAttemptId(attemptId).stream()
				.filter(response -> response.getQuestion().getExamSection().getSkillType() == skillType).toList();
		long answeredCount = responses.stream().filter(this::isAnsweredResponse).count();
		long correctCount = responses.stream().filter(this::isCorrectResponse).count();
		if (answeredCount == 0) {
			return band(0.0);
		}
		if (correctCount == 0) {
			return band(1.0);
		}
		return convertObjectiveRawScoreToBand(correctCount, skillType);
	}

	private BigDecimal convertObjectiveRawScoreToBand(long correctCount, SkillType skillType) {
		if (skillType == SkillType.LISTENING) {
			return convertListeningRawScore(correctCount);
		}
		if (skillType == SkillType.READING) {
			return convertAcademicReadingRawScore(correctCount);
		}
		throw new RuntimeException("Objective band can only be calculated for Listening or Reading");
	}

	private BigDecimal convertListeningRawScore(long correctCount) {
		if (correctCount >= 39) {
			return band(9.0);
		}
		if (correctCount >= 37) {
			return band(8.5);
		}
		if (correctCount >= 35) {
			return band(8.0);
		}
		if (correctCount >= 32) {
			return band(7.5);
		}
		if (correctCount >= 30) {
			return band(7.0);
		}
		if (correctCount >= 26) {
			return band(6.5);
		}
		if (correctCount >= 23) {
			return band(6.0);
		}
		if (correctCount >= 18) {
			return band(5.5);
		}
		if (correctCount >= 16) {
			return band(5.0);
		}
		if (correctCount >= 13) {
			return band(4.5);
		}
		if (correctCount >= 11) {
			return band(4.0);
		}
		if (correctCount >= 8) {
			return band(3.5);
		}
		if (correctCount >= 6) {
			return band(3.0);
		}
		if (correctCount >= 4) {
			return band(2.5);
		}
		if (correctCount == 3) {
			return band(2.0);
		}
		if (correctCount == 2) {
			return band(1.5);
		}
		if (correctCount == 1) {
			return band(1.0);
		}
		return band(0.0);
	}

	private BigDecimal convertAcademicReadingRawScore(long correctCount) {
		if (correctCount >= 39) {
			return band(9.0);
		}
		if (correctCount >= 37) {
			return band(8.5);
		}
		if (correctCount >= 35) {
			return band(8.0);
		}
		if (correctCount >= 33) {
			return band(7.5);
		}
		if (correctCount >= 30) {
			return band(7.0);
		}
		if (correctCount >= 27) {
			return band(6.5);
		}
		if (correctCount >= 23) {
			return band(6.0);
		}
		if (correctCount >= 20) {
			return band(5.5);
		}
		if (correctCount >= 16) {
			return band(5.0);
		}
		if (correctCount >= 13) {
			return band(4.5);
		}
		if (correctCount >= 10) {
			return band(4.0);
		}
		if (correctCount >= 8) {
			return band(3.5);
		}
		if (correctCount >= 6) {
			return band(3.0);
		}
		if (correctCount >= 4) {
			return band(2.5);
		}
		if (correctCount == 3) {
			return band(2.0);
		}
		if (correctCount == 2) {
			return band(1.5);
		}
		if (correctCount == 1) {
			return band(1.0);
		}
		return band(0.0);
	}

	private BigDecimal band(double value) {
		return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
	}

	public BigDecimal calculateWritingBand(Integer attemptId) {
		TestAttempt attempt = findAttempt(attemptId);
		List<Question> writingQuestions = this.questionRepository
				.findByExamSectionExamIdAndExamSectionSkillTypeOrderByExamSectionSectionOrderAscOrderIndexAsc(
						attempt.getExam().getId(), SkillType.WRITING);
		List<UserResponse> writingResponses = this.userResponseRepository
				.findByAttemptIdAndQuestionExamSectionSkillType(attemptId, SkillType.WRITING);
		Map<Integer, UserResponse> responseByQuestionId = writingResponses.stream()
				.collect(Collectors.toMap(response -> response.getQuestion().getId(), Function.identity()));

		BigDecimal task1Score = getWritingTaskScore(writingQuestions, responseByQuestionId, 0);
		BigDecimal task2Score = getWritingTaskScore(writingQuestions, responseByQuestionId, 1);
		return roundToHalfBand((task1Score.doubleValue() + task2Score.doubleValue() * 2.0) / 3.0);
	}

	public BigDecimal calculateOverallBand(Integer attemptId) {
		Map<SkillType, BigDecimal> bandScores = getBandScoreMap(attemptId);
		if (!hasCompleteSkillResults(bandScores)) {
			return null;
		}
		double average = bandScores.values().stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
		return roundToHalfBand(average);
	}

	public Map<SkillType, BigDecimal> getBandScoreMap(Integer attemptId) {
		Map<SkillType, BigDecimal> bandScores = new EnumMap<>(SkillType.class);
		this.skillResultRepository.findByAttemptId(attemptId).forEach(result -> {
			if (result.getBandScore() != null) {
				bandScores.put(result.getSkillType(), result.getBandScore());
			}
		});
		return bandScores;
	}

	public boolean hasCompleteSkillResults(Map<SkillType, BigDecimal> bandScores) {
		return bandScores.get(SkillType.LISTENING) != null && bandScores.get(SkillType.READING) != null
				&& bandScores.get(SkillType.WRITING) != null && bandScores.get(SkillType.SPEAKING) != null;
	}

	public BigDecimal roundToHalfBand(double value) {
		return BigDecimal.valueOf(Math.round(value * 2.0) / 2.0).setScale(1, RoundingMode.HALF_UP);
	}

	private TestAttempt findAttempt(Integer attemptId) {
		return this.testAttemptRepository.findById(attemptId)
				.orElseThrow(() -> new RuntimeException("Test attempt not found"));
	}

	private BigDecimal getWritingTaskScore(List<Question> writingQuestions,
			Map<Integer, UserResponse> responseByQuestionId, int taskIndex) {
		if (writingQuestions.size() <= taskIndex) {
			return BigDecimal.ZERO.setScale(1);
		}
		UserResponse response = responseByQuestionId.get(writingQuestions.get(taskIndex).getId());
		if (response == null) {
			return BigDecimal.ZERO.setScale(1);
		}
		if (response.getAiScore() == null) {
			return BigDecimal.ZERO.setScale(1);
		}
		return response.getAiScore();
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

	private boolean isAnsweredResponse(UserResponse response) {
		return !this.userResponseChoiceRepository.findByResponseId(response.getId()).isEmpty();
	}
}
