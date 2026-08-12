package com.englishApp.exam.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.englishApp.exam.model.Answer;

public interface AnswerRepository extends JpaRepository<Answer, Integer> {
	List<Answer> findByQuestionId(Integer id);
}
