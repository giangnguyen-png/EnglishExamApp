package com.englishApp.exam.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.englishApp.exam.model.Exam;

public interface ExamRepository extends JpaRepository<Exam, Integer> {
	List<Exam> findByTitleContaining(String title);

	List<Exam> findByIsPremiumOnly(boolean isPremium);

	boolean existsByTitle(String title);

	List<Exam> findAllByOrderByCreatedAtDesc();
}
