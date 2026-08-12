package com.englishApp.exam.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.englishApp.exam.model.SkillResult;
import com.englishApp.exam.model.enums.SkillType;

public interface SkillResultRepository extends JpaRepository<SkillResult, Integer> {
	List<SkillResult> findByAttemptId(Integer attemptId);

	Optional<SkillResult> findByAttemptIdAndSkillType(Integer attemptId, SkillType skillType);
}
