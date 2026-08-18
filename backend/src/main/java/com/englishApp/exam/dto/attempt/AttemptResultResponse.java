package com.englishApp.exam.dto.attempt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;

import com.englishApp.exam.model.SkillResult;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.enums.SkillType;

public record AttemptResultResponse(
		Integer attemptId,
		Integer examId,
		String examTitle,
		LocalDateTime startTime,
		LocalDateTime endTime,
		BigDecimal overallBandScore,
		List<SkillResultResponse> skills,
		String aiOverallFeedback) {
	public static AttemptResultResponse from(TestAttempt attempt) {
		List<SkillResult> skillResults = attempt.getSkillResults() == null ? Collections.emptyList()
				: attempt.getSkillResults();
		List<SkillResultResponse> skills = skillResults.stream()
				.sorted(Comparator.comparing(result -> result.getSkillType().ordinal()))
				.map(SkillResultResponse::from)
				.toList();
		return new AttemptResultResponse(
				attempt.getId(),
				attempt.getExam().getId(),
				attempt.getExam().getTitle(),
				attempt.getStartTime(),
				attempt.getEndTime(),
				attempt.getOverallBandScore(),
				skills,
				attempt.getAiOverallFeedback());
	}

	public record SkillResultResponse(
			SkillType skillType,
			BigDecimal bandScore,
			String aiAnalysis) {
		public static SkillResultResponse from(SkillResult result) {
			return new SkillResultResponse(result.getSkillType(), result.getBandScore(), result.getAiAnalysis());
		}
	}
}
