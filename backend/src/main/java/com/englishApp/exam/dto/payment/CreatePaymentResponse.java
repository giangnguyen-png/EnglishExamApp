package com.englishApp.exam.dto.payment;

public record CreatePaymentResponse(
		Integer paymentId,
		String paymentUrl,
		String status,
		String message) {
}
