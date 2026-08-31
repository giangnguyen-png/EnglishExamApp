package com.englishApp.exam.dto.attempt;

import java.util.List;

import com.englishApp.exam.model.enums.QuestionType;
import com.englishApp.exam.model.enums.SkillType;

public record AttemptReviewResponse(
		Integer attemptId,
		String examTitle,
		List<SectionReviewResponse> sections) {

	public record SectionReviewResponse(
			Integer id,
			SkillType skillType,
			int sectionOrder,
			String passageContent,
			String mediaUrl,
			List<QuestionReviewResponse> questions) {
	}

	public record QuestionReviewResponse(
			Integer id,
			String content,
			QuestionType questionType,
			int orderIndex,
			String imageUrl,
			boolean answered,
			boolean correct,
			List<AnswerReviewResponse> answers) {
	}

	public record AnswerReviewResponse(
			Integer id,
			String content,
			boolean selected,
			boolean correct,
			String explanation) {
	}
}
