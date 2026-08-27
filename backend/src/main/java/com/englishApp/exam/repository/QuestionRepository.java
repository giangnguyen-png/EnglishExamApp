package com.englishApp.exam.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.englishApp.exam.model.Question;
import com.englishApp.exam.model.enums.SkillType;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
	List<Question> findByExamSectionIdOrderByOrderIndex(Integer id);

	List<Question> findByExamSectionExamId(Integer examId);

	long countByExamSectionId(Integer id);

	long countByExamSectionExamIdAndExamSectionSkillType(Integer examId, SkillType skillType);

	List<Question> findByExamSectionExamIdAndExamSectionSkillTypeOrderByExamSectionSectionOrderAscOrderIndexAsc(
			Integer examId, SkillType skillType);
}
