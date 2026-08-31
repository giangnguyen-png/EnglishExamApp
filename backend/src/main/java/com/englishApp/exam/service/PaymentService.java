package com.englishApp.exam.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.englishApp.exam.dto.payment.CreatePaymentResponse;
import com.englishApp.exam.model.Payment;

public interface PaymentService {
	CreatePaymentResponse createPremiumPayment(Integer userId);

	Payment createFakePremiumPayment(Integer userId);

	Payment processMomoCallback(Map<String, String> params);

	Payment findById(Integer id);

	List<Payment> findByUser(Integer userId);

	boolean hasPremiumAccess(Integer userId);

	LocalDateTime findPremiumExpiresAt(Integer userId);

	void requirePremium(Integer userId);
}

