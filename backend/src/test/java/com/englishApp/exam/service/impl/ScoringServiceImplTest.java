package com.englishApp.exam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.englishApp.exam.model.Answer;
import com.englishApp.exam.model.Exam;
import com.englishApp.exam.model.ExamSection;
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

@ExtendWith(MockitoExtension.class)
class ScoringServiceImplTest {
	@Mock
	private TestAttemptRepository testAttemptRepository;
	@Mock
	private QuestionRepository questionRepository;
	@Mock
	private UserResponseRepository userResponseRepository;
	@Mock
	private UserResponseChoiceRepository userResponseChoiceRepository;
	@Mock
	private SkillResultRepository skillResultRepository;

	private ScoringServiceImpl service;

	@BeforeEach
	void setUp() {
		this.service = new ScoringServiceImpl(this.testAttemptRepository, this.questionRepository,
				this.userResponseRepository, this.userResponseChoiceRepository, this.skillResultRepository);
	}

	@Test
	void calculateObjectiveBandUsesListeningRawScoreTable() {
		assertObjectiveBand(SkillType.LISTENING, 1, "1.0");
		assertObjectiveBand(SkillType.LISTENING, 40, "9.0");
		assertObjectiveBand(SkillType.LISTENING, 38, "8.5");
		assertObjectiveBand(SkillType.LISTENING, 34, "7.5");
		assertObjectiveBand(SkillType.LISTENING, 30, "7.0");
		assertObjectiveBand(SkillType.LISTENING, 23, "6.0");
		assertObjectiveBand(SkillType.LISTENING, 16, "5.0");
	}

	@Test
	void calculateObjectiveBandUsesAcademicReadingRawScoreTable() {
		assertObjectiveBand(SkillType.READING, 1, "1.0");
		assertObjectiveBand(SkillType.READING, 40, "9.0");
		assertObjectiveBand(SkillType.READING, 38, "8.5");
		assertObjectiveBand(SkillType.READING, 34, "7.5");
		assertObjectiveBand(SkillType.READING, 30, "7.0");
		assertObjectiveBand(SkillType.READING, 27, "6.5");
		assertObjectiveBand(SkillType.READING, 23, "6.0");
		assertObjectiveBand(SkillType.READING, 16, "5.0");
	}

	@Test
	void calculateObjectiveBandRejectsNonObjectiveSkill() {
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.calculateObjectiveBand(1, SkillType.WRITING));

		assertEquals("Objective band can only be calculated for Listening or Reading", exception.getMessage());
	}

	@Test
	void calculateObjectiveBandRejectsNonFortyQuestionObjectiveSections() {
		mockAttempt(SkillType.LISTENING);
		when(this.questionRepository.countByExamSectionExamIdAndExamSectionSkillType(1, SkillType.LISTENING))
				.thenReturn(39L);

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.calculateObjectiveBand(1, SkillType.LISTENING));

		assertEquals("Listening and Reading exams must contain exactly 40 questions", exception.getMessage());
	}

	@Test
	void calculateObjectiveBandReturnsZeroWhenListeningHasNoAnsweredQuestions() {
		assertObjectiveBand(SkillType.LISTENING, 0, 0, "0.0");
	}

	@Test
	void calculateObjectiveBandReturnsOneWhenListeningWasAttemptedButAllAnswersAreWrong() {
		assertObjectiveBand(SkillType.LISTENING, 40, 0, "1.0");
		assertObjectiveBand(SkillType.LISTENING, 5, 0, "1.0");
	}

	@Test
	void calculateObjectiveBandReturnsZeroWhenReadingHasNoAnsweredQuestions() {
		assertObjectiveBand(SkillType.READING, 0, 0, "0.0");
	}

	@Test
	void calculateObjectiveBandReturnsOneWhenReadingWasAttemptedButAllAnswersAreWrong() {
		assertObjectiveBand(SkillType.READING, 40, 0, "1.0");
		assertObjectiveBand(SkillType.READING, 5, 0, "1.0");
	}

	private void assertObjectiveBand(SkillType skillType, int correctCount, String expectedBand) {
		assertObjectiveBand(skillType, correctCount, correctCount, expectedBand);
	}

	private void assertObjectiveBand(SkillType skillType, int answeredCount, int correctCount, String expectedBand) {
		mockAttempt(skillType);
		List<UserResponse> responses = buildResponses(skillType, answeredCount, correctCount);
		when(this.questionRepository.countByExamSectionExamIdAndExamSectionSkillType(1, skillType)).thenReturn(40L);
		when(this.userResponseRepository.findByAttemptId(1)).thenReturn(responses);

		BigDecimal band = this.service.calculateObjectiveBand(1, skillType);

		assertEquals(new BigDecimal(expectedBand), band);
	}

	private void mockAttempt(SkillType skillType) {
		TestAttempt attempt = new TestAttempt();
		Exam exam = new Exam();
		exam.setId(1);
		attempt.setExam(exam);
		when(this.testAttemptRepository.findById(1)).thenReturn(Optional.of(attempt));
	}

	private List<UserResponse> buildResponses(SkillType skillType, int answeredCount, int correctCount) {
		List<UserResponse> responses = new ArrayList<>();
		for (int i = 1; i <= 40; i++) {
			Question question = new Question();
			question.setId(i);
			ExamSection section = new ExamSection();
			section.setSkillType(skillType);
			question.setExamSection(section);

			Answer correctAnswer = new Answer();
			correctAnswer.setId(i);
			correctAnswer.setCorrect(true);
			correctAnswer.setQuestion(question);
			Answer wrongAnswer = new Answer();
			wrongAnswer.setId(i + 1000);
			wrongAnswer.setCorrect(false);
			wrongAnswer.setQuestion(question);
			question.setAnswers(List.of(correctAnswer, wrongAnswer));

			UserResponse response = new UserResponse();
			response.setId(i);
			response.setQuestion(question);
			responses.add(response);
		}

		doAnswer(invocation -> {
			Integer responseId = invocation.getArgument(0);
			if (responseId > answeredCount) {
				return List.of();
			}
			UserResponseChoice choice = new UserResponseChoice();
			int answerIndex = responseId <= correctCount ? 0 : 1;
			choice.setAnswer(responses.get(responseId - 1).getQuestion().getAnswers().get(answerIndex));
			return List.of(choice);
		}).when(this.userResponseChoiceRepository).findByResponseId(anyInt());

		return responses;
	}
}
