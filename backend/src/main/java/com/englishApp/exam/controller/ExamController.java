package com.englishApp.exam.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.englishApp.exam.dto.attempt.StartAttemptResponse;
import com.englishApp.exam.dto.exam.ExamListResponse;
import com.englishApp.exam.dto.exam.ExamSummaryResponse;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.User;
import com.englishApp.exam.service.ExamService;
import com.englishApp.exam.service.TestAttemptService;
import com.englishApp.exam.service.UserService;

@RestController
@RequestMapping("/api/exams")
public class ExamController {
	private final ExamService examService;
	private final TestAttemptService testAttemptService;
	private final UserService userService;

	public ExamController(ExamService examService, TestAttemptService testAttemptService, UserService userService) {
		this.examService = examService;
		this.testAttemptService = testAttemptService;
		this.userService = userService;
	}

	@GetMapping
	public ResponseEntity<List<ExamListResponse>> findAll() {
		List<ExamListResponse> exams = this.examService.findAll().stream()
				.map(ExamListResponse::from)
				.toList();
		return ResponseEntity.ok(exams);
	}

	@GetMapping("/{examId}")
	public ResponseEntity<ExamSummaryResponse> findById(@PathVariable Integer examId) {
		return ResponseEntity.ok(ExamSummaryResponse.from(this.examService.findById(examId)));
	}

	@PostMapping("/{examId}/attempts")
	public ResponseEntity<StartAttemptResponse> startNormalAttempt(@PathVariable Integer examId,
			Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		TestAttempt attempt = this.testAttemptService.startAttempt(currentUser.getId(), examId, null);
		return ResponseEntity.ok(StartAttemptResponse.from(attempt));
	}

	private User getCurrentUser(Authentication authentication) {
		return this.userService.findByUsername(authentication.getName());
	}
}
