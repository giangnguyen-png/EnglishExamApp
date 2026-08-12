package com.englishApp.exam.service;

import java.util.List;

import com.englishApp.exam.model.Exam;
import com.englishApp.exam.model.enums.SkillType;

public interface ExamService {
	Exam createExam(Exam exam);

	Exam updateExam(Integer id, Exam updatedExam);

	void deleteExam(Integer id);

	Exam findById(Integer id);

	List<Exam> findAll();

	List<Exam> findBySkillType(SkillType skillType);
}
