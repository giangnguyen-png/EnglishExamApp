package com.englishApp.exam.dto.exam;

import java.util.Comparator;
import java.util.List;

import com.englishApp.exam.model.Answer;
import com.englishApp.exam.model.Exam;
import com.englishApp.exam.model.ExamSection;
import com.englishApp.exam.model.Question;
import com.englishApp.exam.model.enums.QuestionType;
import com.englishApp.exam.model.enums.SkillType;

public record ExamDetailResponse(
		Integer id,
		String title,
		String description,
		boolean premiumOnly,
		List<SectionResponse> sections) {
	public static ExamDetailResponse from(Exam exam) {
		List<SectionResponse> sections = exam.getExamSections().stream()
				.sorted(Comparator.comparingInt(ExamSection::getSectionOrder))
				.map(SectionResponse::from)
				.toList();
		return new ExamDetailResponse(exam.getId(), exam.getTitle(), exam.getDescription(), exam.isPremiumOnly(),
				sections);
	}

	public record SectionResponse(
			Integer id,
			SkillType skillType,
			String passageContent,
			String mediaUrl,
			int sectionOrder,
			List<QuestionResponse> questions) {
		public static SectionResponse from(ExamSection section) {
			List<QuestionResponse> questions = section.getQuestions().stream()
					.sorted(Comparator.comparingInt(Question::getOrderIndex))
					.map(QuestionResponse::from)
					.toList();
			return new SectionResponse(section.getId(), section.getSkillType(), section.getPassageContent(),
					section.getMediaUrl(), section.getSectionOrder(), questions);
		}
	}

	public record QuestionResponse(
			Integer id,
			QuestionType questionType,
			String content,
			int orderIndex,
			List<AnswerResponse> answers) {
		public static QuestionResponse from(Question question) {
			List<AnswerResponse> answers = question.getAnswers().stream()
					.sorted(Comparator.comparing(Answer::getId))
					.map(AnswerResponse::from)
					.toList();
			return new QuestionResponse(question.getId(), question.getQuestionType(), question.getContent(),
					question.getOrderIndex(), answers);
		}
	}

	public record AnswerResponse(
			Integer id,
			String content) {
		public static AnswerResponse from(Answer answer) {
			return new AnswerResponse(answer.getId(), answer.getContent());
		}
	}
}
