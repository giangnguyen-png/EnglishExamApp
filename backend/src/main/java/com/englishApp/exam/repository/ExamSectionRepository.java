package com.englishApp.exam.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.englishApp.exam.model.ExamSection;

public interface ExamSectionRepository extends JpaRepository<ExamSection, Integer> {
	List<ExamSection> findByExamIdOrderBySectionOrder(Integer examId);
}
