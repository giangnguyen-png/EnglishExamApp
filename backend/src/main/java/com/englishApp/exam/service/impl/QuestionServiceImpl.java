package com.englishApp.exam.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.englishApp.exam.model.ExamSection;
import com.englishApp.exam.model.Question;
import com.englishApp.exam.repository.ExamSectionRepository;
import com.englishApp.exam.repository.QuestionRepository;
import com.englishApp.exam.service.QuestionService;

@Service
public class QuestionServiceImpl implements QuestionService {
	private final QuestionRepository questionRepository;
	private final ExamSectionRepository examSectionRepository;

	public QuestionServiceImpl(QuestionRepository questionRepository, ExamSectionRepository examSectionRepository) {
		this.questionRepository = questionRepository;
		this.examSectionRepository = examSectionRepository;
	}

	public Question createQuestion(Integer sectionId, Question question) {
		ExamSection section = this.examSectionRepository.findById(sectionId)
				.orElseThrow(() -> new RuntimeException("Exam section not found"));
		validateQuestion(question);
		question.setExamSection(section);
		return this.questionRepository.save(question);
	}

	public Question updateQuestion(Integer id, Question updatedQuestion) {
		Question question = this.findById(id);
		validateQuestion(updatedQuestion);
		question.setQuestionType(updatedQuestion.getQuestionType());
		question.setContent(updatedQuestion.getContent());
		question.setPoints(updatedQuestion.getPoints());
		question.setOrderIndex(updatedQuestion.getOrderIndex());
		return this.questionRepository.save(question);
	}

	public void deleteQuestion(Integer id) {
		Question question = this.findById(id);
		this.questionRepository.delete(question);
	}

	public Question findById(Integer id) {
		return this.questionRepository.findById(id).orElseThrow(() -> new RuntimeException("Question not found"));
	}

	public List<Question> findAll() {
		return this.questionRepository.findAll();
	}

	public List<Question> findBySection(Integer sectionId) {
		if (!this.examSectionRepository.existsById(sectionId)) {
			throw new RuntimeException("Exam section not found");
		}
		return this.questionRepository.findByExamSectionIdOrderByOrderIndex(sectionId);
	}

	private void validateQuestion(Question question) {
		if (question == null || question.getQuestionType() == null) {
			throw new RuntimeException("Question type is required");
		}
		if (question.getContent() == null || question.getContent().isBlank()) {
			throw new RuntimeException("Question content is required");
		}
		if (question.getPoints() < 0) {
			throw new RuntimeException("Question points must be greater than or equal to 0");
		}
	}
}
