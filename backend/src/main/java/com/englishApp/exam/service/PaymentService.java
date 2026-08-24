package com.englishApp.exam.service;

import java.util.List;

import com.englishApp.exam.model.Payment;
import com.englishApp.exam.model.enums.PaymentStatus;

public interface PaymentService {
	Payment createPayment(Payment payment);

	Payment createFakePremiumPayment(Integer userId);

	Payment updatePaymentStatus(String transactionId, PaymentStatus status);

	Payment findByTransactionId(String transactionId);

	List<Payment> findByUser(Integer userId);

	boolean hasPremiumAccess(Integer userId);

	void requirePremium(Integer userId);
}

