package com.englishApp.exam.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.englishApp.exam.dto.attempt.AttemptHistoryResponse;
import com.englishApp.exam.dto.attempt.AttemptResultResponse;
import com.englishApp.exam.dto.exam.ExamDetailResponse;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.User;
import com.englishApp.exam.service.TestAttemptService;
import com.englishApp.exam.service.UserService;

@RestController
@RequestMapping("/api/attempts")
public class AttemptController {
	private final TestAttemptService testAttemptService;
	private final UserService userService;

	public AttemptController(TestAttemptService testAttemptService, UserService userService) {
		this.testAttemptService = testAttemptService;
		this.userService = userService;
	}

	@GetMapping
	public ResponseEntity<List<AttemptHistoryResponse>> findCurrentUserAttempts(Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		List<AttemptHistoryResponse> attempts = this.testAttemptService.findByUser(currentUser.getId()).stream()
				.map(AttemptHistoryResponse::from)
				.toList();
		return ResponseEntity.ok(attempts);
	}

	@GetMapping("/{attemptId}")
	public ResponseEntity<AttemptResultResponse> findById(@PathVariable Integer attemptId,
			Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		TestAttempt attempt = this.testAttemptService.findById(attemptId);
		validateAttemptOwner(attempt, currentUser);
		return ResponseEntity.ok(AttemptResultResponse.from(attempt));
	}

	@GetMapping("/{attemptId}/exam")
	public ResponseEntity<ExamDetailResponse> findAttemptExam(@PathVariable Integer attemptId,
			Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		TestAttempt attempt = this.testAttemptService.findById(attemptId);
		validateAttemptOwner(attempt, currentUser);
		if (attempt.getEndTime() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submitted attempts cannot load active exam content");
		}
		this.testAttemptService.validateExamReadyForAttempt(attempt.getExam().getId());
		return ResponseEntity.ok(ExamDetailResponse.from(attempt.getExam()));
	}

	@PostMapping("/{attemptId}/submit")
	public ResponseEntity<AttemptResultResponse> submitAttempt(@PathVariable Integer attemptId,
			Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		TestAttempt attempt = this.testAttemptService.findById(attemptId);
		validateAttemptOwner(attempt, currentUser);
		TestAttempt submittedAttempt = this.testAttemptService.submitAttempt(attemptId);
		return ResponseEntity.ok(AttemptResultResponse.from(submittedAttempt));
	}

	@PostMapping("/{attemptId}/force-submit")
	public ResponseEntity<AttemptResultResponse> forceSubmitAttempt(@PathVariable Integer attemptId,
			Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		TestAttempt attempt = this.testAttemptService.findById(attemptId);
		validateAttemptOwner(attempt, currentUser);
		TestAttempt submittedAttempt = this.testAttemptService.forceSubmitAttempt(attemptId);
		return ResponseEntity.ok(AttemptResultResponse.from(submittedAttempt));
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
