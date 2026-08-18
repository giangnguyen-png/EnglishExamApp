package com.englishApp.exam.dto.exam;

import com.englishApp.exam.model.Exam;

public record ExamListResponse(
		Integer id,
		String title,
		String description,
		boolean premiumOnly) {
	public static ExamListResponse from(Exam exam) {
		return new ExamListResponse(exam.getId(), exam.getTitle(), exam.getDescription(), exam.isPremiumOnly());
	}
}
