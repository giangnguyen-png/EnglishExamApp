package com.englishApp.exam.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.englishApp.exam.dto.payment.PremiumStatusResponse;
import com.englishApp.exam.model.User;
import com.englishApp.exam.service.PaymentService;
import com.englishApp.exam.service.UserService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
	private final PaymentService paymentService;
	private final UserService userService;

	public PaymentController(PaymentService paymentService, UserService userService) {
		this.paymentService = paymentService;
		this.userService = userService;
	}

	@GetMapping("/premium-status")
	public ResponseEntity<PremiumStatusResponse> getPremiumStatus(Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		boolean premium = this.paymentService.hasPremiumAccess(currentUser.getId());
		return ResponseEntity.ok(new PremiumStatusResponse(premium, premium ? "Tài khoản đã có quyền Premium" : null));
	}

	@PostMapping("/fake-premium")
	public ResponseEntity<PremiumStatusResponse> createFakePremiumPayment(Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		boolean alreadyPremium = this.paymentService.hasPremiumAccess(currentUser.getId());
		this.paymentService.createFakePremiumPayment(currentUser.getId());
		String message = alreadyPremium ? "Tài khoản đã có quyền Premium" : "Thanh toán thành công. Tài khoản Premium đã được kích hoạt.";
		return ResponseEntity.ok(new PremiumStatusResponse(true, message));
	}

	private User getCurrentUser(Authentication authentication) {
		return this.userService.findByUsername(authentication.getName());
	}
}
