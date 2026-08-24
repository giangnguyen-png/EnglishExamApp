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

	boolean existsByUserIdAndStatus(Integer userId, PaymentStatus status);

	Optional<Payment> findFirstByUserIdAndStatus(Integer userId, PaymentStatus status);
}


