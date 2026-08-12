package com.englishApp.exam.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.englishApp.exam.model.Exam;
import com.englishApp.exam.model.enums.SkillType;
import com.englishApp.exam.repository.ExamRepository;
import com.englishApp.exam.service.ExamService;

@Service
public class ExamServiceImpl implements ExamService {
	private final ExamRepository examRepository;

	public ExamServiceImpl(ExamRepository examRepository) {
		this.examRepository = examRepository;
	}

	public Exam createExam(Exam exam) {
		validateExam(exam);
		if (this.examRepository.existsByTitle(exam.getTitle())) {
			throw new RuntimeException("Exam title already exists");
		}
		return this.examRepository.save(exam);
	}

	public Exam updateExam(Integer id, Exam updatedExam) {
		Exam exam = this.findById(id);
		validateExam(updatedExam);
		if (!exam.getTitle().equals(updatedExam.getTitle()) && this.examRepository.existsByTitle(updatedExam.getTitle())) {
			throw new RuntimeException("Exam title already exists");
		}
		exam.setTitle(updatedExam.getTitle());
		exam.setDescription(updatedExam.getDescription());
		exam.setPremiumOnly(updatedExam.isPremiumOnly());
		return this.examRepository.save(exam);
	}

	public void deleteExam(Integer id) {
		Exam exam = this.findById(id);
		this.examRepository.delete(exam);
	}

	public Exam findById(Integer id) {
		return this.examRepository.findById(id).orElseThrow(() -> new RuntimeException("Exam not found"));
	}

	public List<Exam> findAll() {
		return this.examRepository.findAllByOrderByCreatedAtDesc();
	}

	public List<Exam> findBySkillType(SkillType skillType) {
		return this.examRepository.findAll().stream()
				.filter(exam -> exam.getExamSections() != null && exam.getExamSections().stream()
						.anyMatch(section -> section.getSkillType() == skillType))
				.toList();
	}

	private void validateExam(Exam exam) {
		if (exam == null || exam.getTitle() == null || exam.getTitle().isBlank()) {
			throw new RuntimeException("Exam title is required");
		}
	}
}
