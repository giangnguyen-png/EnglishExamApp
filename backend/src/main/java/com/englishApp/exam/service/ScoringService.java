package com.englishApp.exam.service;

import java.math.BigDecimal;
import java.util.Map;

import com.englishApp.exam.model.enums.SkillType;

public interface ScoringService {
	BigDecimal calculateObjectiveBand(Integer attemptId, SkillType skillType);

	BigDecimal calculateWritingBand(Integer attemptId);

	BigDecimal calculateOverallBand(Integer attemptId);

	Map<SkillType, BigDecimal> getBandScoreMap(Integer attemptId);

	boolean hasCompleteSkillResults(Map<SkillType, BigDecimal> bandScores);

	BigDecimal roundToHalfBand(double value);
}
