package com.englishApp.exam.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.englishApp.exam.model.Payment;
import com.englishApp.exam.model.enums.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
	List<Payment> findByUserId(Integer userId);

	Optional<Payment> findByTransactionId(String transactionId);

	List<Payment> findByStatus(PaymentStatus status);

	List<Payment> findByUserIdAndStatus(Integer userId, PaymentStatus status);
}


