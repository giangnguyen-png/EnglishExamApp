package com.englishApp.exam.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.englishApp.exam.dto.userresponse.QuestionSubmissionResponse;
import com.englishApp.exam.dto.userresponse.SaveAnswerRequest;
import com.englishApp.exam.dto.userresponse.SaveAnswerResponse;
import com.englishApp.exam.dto.userresponse.SpeakingSubmissionResponse;
import com.englishApp.exam.dto.userresponse.WritingRequest;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.User;
import com.englishApp.exam.service.TestAttemptService;
import com.englishApp.exam.service.UserResponseService;
import com.englishApp.exam.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/attempts")
public class UserResponseController {
	private final UserResponseService userResponseService;
	private final TestAttemptService testAttemptService;
	private final UserService userService;

	public UserResponseController(UserResponseService userResponseService, TestAttemptService testAttemptService,
			UserService userService) {
		this.userResponseService = userResponseService;
		this.testAttemptService = testAttemptService;
		this.userService = userService;
	}

	@PutMapping("/{attemptId}/questions/{questionId}/answer")
	public ResponseEntity<SaveAnswerResponse> saveAnswer(@PathVariable Integer attemptId,
			@PathVariable Integer questionId, @RequestBody SaveAnswerRequest request, Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		validateAttemptOwner(this.testAttemptService.findById(attemptId), currentUser);
		this.userResponseService.saveAnswer(attemptId, questionId, request.answerIds());
		return ResponseEntity.ok(new SaveAnswerResponse(attemptId, questionId, request.answerIds(), true));
	}

	@PutMapping("/{attemptId}/questions/{questionId}/writing")
	public ResponseEntity<QuestionSubmissionResponse> submitWriting(@PathVariable Integer attemptId,
			@PathVariable Integer questionId, @Valid @RequestBody WritingRequest request, Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		validateAttemptOwner(this.testAttemptService.findById(attemptId), currentUser);
		this.userResponseService.submitWriting(attemptId, questionId, request.textContent());
		return ResponseEntity.ok(new QuestionSubmissionResponse(questionId, true));
	}

	@PutMapping(value = "/{attemptId}/questions/{questionId}/speaking", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<SpeakingSubmissionResponse> submitSpeaking(@PathVariable Integer attemptId,
			@PathVariable Integer questionId, @RequestPart("audioFile") MultipartFile audioFile,
			Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		validateAttemptOwner(this.testAttemptService.findById(attemptId), currentUser);
		this.userResponseService.submitSpeaking(attemptId, questionId, audioFile);
		return ResponseEntity.ok(new SpeakingSubmissionResponse(questionId, true));
	}

	private User getCurrentUser(Authentication authentication) {
		return this.userService.findByUsername(authentication.getName());
	}

	private void validateAttemptOwner(TestAttempt attempt, User currentUser) {
		if (!attempt.getUser().getId().equals(currentUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Attempt does not belong to current user");
		}
	}
}
