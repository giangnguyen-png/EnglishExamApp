package com.englishApp.exam.controller.admin;

import java.math.BigDecimal;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.englishApp.exam.model.enums.PaymentStatus;
import com.englishApp.exam.repository.ExamRepository;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.PaymentRepository;
import com.englishApp.exam.repository.UserRepository;

@Controller
public class AdminDashboardController {
	private final UserRepository userRepository;
	private final ExamRepository examRepository;
	private final MockSessionRepository mockSessionRepository;
	private final PaymentRepository paymentRepository;

	public AdminDashboardController(UserRepository userRepository, ExamRepository examRepository,
			MockSessionRepository mockSessionRepository, PaymentRepository paymentRepository) {
		this.userRepository = userRepository;
		this.examRepository = examRepository;
		this.mockSessionRepository = mockSessionRepository;
		this.paymentRepository = paymentRepository;
	}

	@GetMapping({ "/admin", "/admin/dashboard" })
	public String dashboard(Model model) {
		BigDecimal successTotal = this.paymentRepository.findByStatus(PaymentStatus.SUCCESS).stream()
				.map(payment -> payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		model.addAttribute("userCount", this.userRepository.count());
		model.addAttribute("examCount", this.examRepository.count());
		model.addAttribute("mockSessionCount", this.mockSessionRepository.count());
		model.addAttribute("paymentCount", this.paymentRepository.count());
		model.addAttribute("successPaymentTotal", successTotal);
		model.addAttribute("recentPayments",
				this.paymentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().limit(5).toList());
		return "admin/dashboard";
	}

	@GetMapping("/admin/login")
	public String login() {
		return "admin/login";
	}
}
