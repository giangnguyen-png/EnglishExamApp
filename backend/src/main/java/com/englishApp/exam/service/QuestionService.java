package com.englishApp.exam.service;

import java.util.List;

import com.englishApp.exam.model.Question;

public interface QuestionService {
	Question createQuestion(Integer sectionId, Question question);

	Question updateQuestion(Integer id, Question updatedQuestion);

	void deleteQuestion(Integer id);

	Question findById(Integer id);

	List<Question> findAll();

	List<Question> findBySection(Integer sectionId);
}
