package com.englishApp.exam.dto.payment;

public record PaymentStatusResponse(
		Integer paymentId,
		String status,
		String message) {
}
