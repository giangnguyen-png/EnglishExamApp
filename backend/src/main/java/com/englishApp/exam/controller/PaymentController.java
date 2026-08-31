package com.englishApp.exam.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.englishApp.exam.dto.payment.CreatePaymentResponse;
import com.englishApp.exam.dto.payment.PaymentStatusResponse;
import com.englishApp.exam.dto.payment.PremiumStatusResponse;
import com.englishApp.exam.model.Payment;
import com.englishApp.exam.model.User;
import com.englishApp.exam.model.enums.PaymentStatus;
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
		return ResponseEntity.ok(new PremiumStatusResponse(premium,
				this.paymentService.findPremiumExpiresAt(currentUser.getId()),
				premium ? "Tài khoản Premium đang hoạt động" : null));
	}

	@PostMapping("/premium")
	public ResponseEntity<CreatePaymentResponse> createPremiumPayment(Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		return ResponseEntity.ok(this.paymentService.createPremiumPayment(currentUser.getId()));
	}

	@GetMapping("/{paymentId}/status")
	public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable Integer paymentId,
			Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		Payment payment = this.paymentService.findById(paymentId);
		if (!payment.getUser().getId().equals(currentUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payment does not belong to current user");
		}
		return ResponseEntity.ok(new PaymentStatusResponse(payment.getId(), payment.getStatus().name(),
				paymentStatusMessage(payment.getStatus())));
	}

	@GetMapping("/momo-return")
	public ResponseEntity<String> momoReturn(@RequestParam Map<String, String> params) {
		try {
			Payment payment = this.paymentService.processMomoCallback(params);
			return ResponseEntity.ok("""
					<html><body>
					<h2>IELTS AI Practice App</h2>
					<p>%s</p>
					<p>Bạn có thể quay lại ứng dụng để kiểm tra Premium.</p>
					</body></html>
					""".formatted(paymentStatusMessage(payment.getStatus())));
		} catch (RuntimeException error) {
			return ResponseEntity.badRequest().body("""
					<html><body>
					<h2>IELTS AI Practice App</h2>
					<p>Thanh toán thất bại</p>
					<p>Bạn có thể quay lại ứng dụng để kiểm tra Premium.</p>
					</body></html>
					""");
		}
	}

	@PostMapping("/momo-ipn")
	public ResponseEntity<Void> momoIpn(@RequestBody Map<String, String> params) {
		try {
			this.paymentService.processMomoCallback(params);
			return ResponseEntity.noContent().build();
		} catch (RuntimeException error) {
			return ResponseEntity.badRequest().build();
		}
	}

	@PostMapping("/fake-premium")
	public ResponseEntity<PremiumStatusResponse> createFakePremiumPayment(Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		boolean alreadyPremium = this.paymentService.hasPremiumAccess(currentUser.getId());
		this.paymentService.createFakePremiumPayment(currentUser.getId());
		String message = alreadyPremium ? "Tài khoản đã có quyền Premium" : "Thanh toán thành công. Tài khoản Premium đã được kích hoạt.";
		return ResponseEntity.ok(new PremiumStatusResponse(true,
				this.paymentService.findPremiumExpiresAt(currentUser.getId()), message));
	}

	private User getCurrentUser(Authentication authentication) {
		return this.userService.findByUsername(authentication.getName());
	}

	private String paymentStatusMessage(PaymentStatus status) {
		if (status == PaymentStatus.SUCCESS) {
			return "Thanh toán thành công";
		}
		if (status == PaymentStatus.FAILED) {
			return "Thanh toán thất bại";
		}
		return "Đang chờ thanh toán";
	}
}
