package com.englishApp.exam.dto.exam;

import java.util.Comparator;
import java.util.List;

import com.englishApp.exam.model.Exam;
import com.englishApp.exam.model.ExamSection;
import com.englishApp.exam.model.enums.SkillType;

public record ExamSummaryResponse(
		Integer id,
		String title,
		String description,
		boolean premiumOnly,
		List<SectionSummaryResponse> sections) {
	public static ExamSummaryResponse from(Exam exam) {
		List<SectionSummaryResponse> sections = exam.getExamSections().stream()
				.sorted(Comparator.comparingInt(ExamSection::getSectionOrder))
				.map(SectionSummaryResponse::from)
				.toList();
		return new ExamSummaryResponse(exam.getId(), exam.getTitle(), exam.getDescription(), exam.isPremiumOnly(),
				sections);
	}

	public record SectionSummaryResponse(
			Integer id,
			SkillType skillType,
			int sectionOrder,
			int questionCount) {
		public static SectionSummaryResponse from(ExamSection section) {
			int questionCount = section.getQuestions() == null ? 0 : section.getQuestions().size();
			return new SectionSummaryResponse(section.getId(), section.getSkillType(), section.getSectionOrder(),
					questionCount);
		}
	}
}
