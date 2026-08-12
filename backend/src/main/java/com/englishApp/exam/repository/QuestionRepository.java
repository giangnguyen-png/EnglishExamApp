package com.englishApp.exam.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.englishApp.exam.model.Question;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
	List<Question> findByExamSectionIdOrderByOrderIndex(Integer id);

	long countByExamSectionId(Integer id);
}
