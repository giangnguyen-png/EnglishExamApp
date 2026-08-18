package com.englishApp.exam.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.englishApp.exam.model.UserResponse;
import com.englishApp.exam.model.enums.SkillType;

public interface UserResponseRepository extends JpaRepository<UserResponse, Integer> {
	List<UserResponse> findByAttemptId(Integer attemptId);

	List<UserResponse> findByQuestionId(Integer questionId);

	Optional<UserResponse> findByAttemptIdAndQuestionId(Integer attemptId, Integer questionId);

	List<UserResponse> findByAttemptSessionIdAndQuestionExamSectionSkillType(Integer sessionId, SkillType skillType);

	List<UserResponse> findByAttemptSessionIdAndAttemptEndTimeIsNotNullAndQuestionExamSectionSkillType(
			Integer sessionId, SkillType skillType);

	List<UserResponse> findByAttemptIdAndQuestionExamSectionSkillType(Integer attemptId, SkillType skillType);
}
