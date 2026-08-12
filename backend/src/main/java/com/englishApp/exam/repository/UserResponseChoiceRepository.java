package com.englishApp.exam.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.englishApp.exam.model.UserResponseChoice;

public interface UserResponseChoiceRepository extends JpaRepository<UserResponseChoice, Integer> {
	List<UserResponseChoice> findByResponseId(Integer responseId);
}
