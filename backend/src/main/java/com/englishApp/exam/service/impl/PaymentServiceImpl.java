package com.englishApp.exam.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.englishApp.exam.model.Payment;
import com.englishApp.exam.model.enums.PaymentStatus;
import com.englishApp.exam.repository.PaymentRepository;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.service.PaymentService;

@Service
public class PaymentServiceImpl implements PaymentService {
	private final PaymentRepository paymentRepository;
	private final UserRepository userRepository;

	public PaymentServiceImpl(PaymentRepository paymentRepository, UserRepository userRepository) {
		this.paymentRepository = paymentRepository;
		this.userRepository = userRepository;
	}

	public Payment createPayment(Payment payment) {
		if (payment == null || payment.getUser() == null || payment.getUser().getId() == null) {
			throw new RuntimeException("Payment user is required");
		}
		if (!this.userRepository.existsById(payment.getUser().getId())) {
			throw new RuntimeException("User not found");
		}
		if (payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
			throw new RuntimeException("Transaction id is required");
		}
		if (this.paymentRepository.findByTransactionId(payment.getTransactionId()).isPresent()) {
			throw new RuntimeException("Transaction id already exists");
		}
		if (payment.getStatus() == null) {
			payment.setStatus(PaymentStatus.PENDING);
		}
		return this.paymentRepository.save(payment);
	}

	public Payment updatePaymentStatus(String transactionId, PaymentStatus status) {
		Payment payment = this.findByTransactionId(transactionId);
		if (status == null) {
			throw new RuntimeException("Payment status is required");
		}
		payment.setStatus(status);
		return this.paymentRepository.save(payment);
	}

	public Payment findByTransactionId(String transactionId) {
		return this.paymentRepository.findByTransactionId(transactionId)
				.orElseThrow(() -> new RuntimeException("Payment not found"));
	}

	public List<Payment> findByUser(Integer userId) {
		if (!this.userRepository.existsById(userId)) {
			throw new RuntimeException("User not found");
		}
		return this.paymentRepository.findByUserId(userId);
	}
}
