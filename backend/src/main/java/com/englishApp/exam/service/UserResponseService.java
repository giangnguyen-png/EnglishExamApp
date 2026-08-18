package com.englishApp.exam.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.englishApp.exam.model.UserResponse;

public interface UserResponseService {
	UserResponse saveAnswer(Integer attemptId, Integer questionId, List<Integer> answerIds);

	UserResponse submitWriting(Integer attemptId, Integer questionId, String textContent);

	UserResponse submitSpeaking(Integer attemptId, Integer questionId, MultipartFile audioFile);

	List<UserResponse> findByAttempt(Integer attemptId);

	List<UserResponse> findSpeakingResponsesBySession(Integer sessionId);
}
