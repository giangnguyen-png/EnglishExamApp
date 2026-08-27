package com.englishApp.exam.controller.admin;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.englishApp.exam.repository.PaymentRepository;

@Controller
public class AdminPaymentController {
	private final PaymentRepository paymentRepository;

	public AdminPaymentController(PaymentRepository paymentRepository) {
		this.paymentRepository = paymentRepository;
	}

	@GetMapping("/admin/payments")
	public String list(Model model) {
		model.addAttribute("payments", this.paymentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
		return "admin/payments/list";
	}
}
