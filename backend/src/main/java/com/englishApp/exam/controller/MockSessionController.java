package com.englishApp.exam.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.englishApp.exam.dto.mocksession.MockSessionSummaryResponse;
import com.englishApp.exam.dto.mocksession.MySessionRegistrationResponse;
import com.englishApp.exam.dto.mocksession.PremiumStartAttemptResponse;
import com.englishApp.exam.dto.mocksession.SessionRegistrationResponse;
import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.model.SessionRegistration;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.User;
import com.englishApp.exam.service.MockSessionService;
import com.englishApp.exam.service.PaymentService;
import com.englishApp.exam.service.SessionRegistrationService;
import com.englishApp.exam.service.TestAttemptService;
import com.englishApp.exam.service.UserService;

@RestController
@RequestMapping("/api/mock-sessions")
public class MockSessionController {
	private final MockSessionService mockSessionService;
	private final SessionRegistrationService sessionRegistrationService;
	private final TestAttemptService testAttemptService;
	private final PaymentService paymentService;
	private final UserService userService;

	public MockSessionController(MockSessionService mockSessionService,
			SessionRegistrationService sessionRegistrationService, TestAttemptService testAttemptService,
			PaymentService paymentService, UserService userService) {
		this.mockSessionService = mockSessionService;
		this.sessionRegistrationService = sessionRegistrationService;
		this.testAttemptService = testAttemptService;
		this.paymentService = paymentService;
		this.userService = userService;
	}

	@GetMapping("/available")
	public ResponseEntity<List<MockSessionSummaryResponse>> findAvailableSessions(Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		this.paymentService.requirePremium(currentUser.getId());
		List<MockSessionSummaryResponse> sessions = this.mockSessionService.findAvailableSessions().stream()
				.sorted(Comparator.comparing(MockSession::getStartTime))
				.map(MockSessionSummaryResponse::from)
				.toList();
		return ResponseEntity.ok(sessions);
	}

	@PostMapping("/{sessionId}/registrations")
	public ResponseEntity<SessionRegistrationResponse> registerSession(@PathVariable Integer sessionId,
			Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		this.paymentService.requirePremium(currentUser.getId());
		SessionRegistration registration = this.sessionRegistrationService.registerSession(sessionId, currentUser.getId());
		return ResponseEntity.ok(SessionRegistrationResponse.from(registration));
	}

	@GetMapping("/registrations/me")
	public ResponseEntity<List<MySessionRegistrationResponse>> findMyRegistrations(Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		this.paymentService.requirePremium(currentUser.getId());
		List<MySessionRegistrationResponse> registrations = this.sessionRegistrationService
				.findByUser(currentUser.getId()).stream()
				.sorted(Comparator.comparing(registration -> registration.getSession().getStartTime()))
				.map(MySessionRegistrationResponse::from)
				.toList();
		return ResponseEntity.ok(registrations);
	}

	@GetMapping("/{sessionId}")
	public ResponseEntity<MockSessionSummaryResponse> findRegisteredSession(@PathVariable Integer sessionId,
			Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		this.paymentService.requirePremium(currentUser.getId());
		boolean registered = this.sessionRegistrationService.findByUser(currentUser.getId()).stream()
				.anyMatch(registration -> registration.getSession().getId().equals(sessionId));
		if (!registered) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not registered for this session");
		}
		return ResponseEntity.ok(MockSessionSummaryResponse.from(this.mockSessionService.findById(sessionId)));
	}

	@DeleteMapping("/registrations/{registrationId}")
	public ResponseEntity<Void> cancelRegistration(@PathVariable Integer registrationId, Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		this.paymentService.requirePremium(currentUser.getId());
		SessionRegistration registration = this.sessionRegistrationService.findById(registrationId);
		validateRegistrationOwner(registration, currentUser);
		this.sessionRegistrationService.cancelRegistration(registrationId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{sessionId}/attempts")
	public ResponseEntity<PremiumStartAttemptResponse> startPremiumAttempt(@PathVariable Integer sessionId,
			Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		this.paymentService.requirePremium(currentUser.getId());
		MockSession session = this.mockSessionService.findById(sessionId);
		TestAttempt attempt = this.testAttemptService.startAttempt(currentUser.getId(), session.getExam().getId(),
				sessionId);
		return ResponseEntity.ok(PremiumStartAttemptResponse.from(attempt));
	}

	private User getCurrentUser(Authentication authentication) {
		return this.userService.findByUsername(authentication.getName());
	}

	private void validateRegistrationOwner(SessionRegistration registration, User currentUser) {
		if (!registration.getUser().getId().equals(currentUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration does not belong to current user");
		}
	}
}
