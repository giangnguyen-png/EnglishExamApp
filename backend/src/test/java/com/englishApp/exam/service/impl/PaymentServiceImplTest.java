package com.englishApp.exam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.englishApp.exam.dto.payment.CreatePaymentResponse;
import com.englishApp.exam.model.Payment;
import com.englishApp.exam.model.User;
import com.englishApp.exam.model.enums.PaymentMethod;
import com.englishApp.exam.model.enums.PaymentStatus;
import com.englishApp.exam.repository.PaymentRepository;
import com.englishApp.exam.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
	private static final String ACCESS_KEY = "test-access";
	private static final String SECRET_KEY = "sandbox-secret";
	private static final String PARTNER_CODE = "MOMO";
	private static final String ORDER_ID = "MOMOPREM123";

	@Mock
	private PaymentRepository paymentRepository;
	@Mock
	private UserRepository userRepository;

	private PaymentServiceImpl service;
	private PaymentServiceImpl.MomoCreateResponse momoCreateResponse;
	private PaymentServiceImpl.MomoCreateRequest momoCreateRequest;

	@BeforeEach
	void setUp() {
		this.service = new PaymentServiceImpl(this.paymentRepository, this.userRepository) {
			@Override
			PaymentServiceImpl.MomoCreateResponse postMomoCreateRequest(
					PaymentServiceImpl.MomoCreateRequest request) {
				momoCreateRequest = request;
				return momoCreateResponse;
			}
		};
		ReflectionTestUtils.setField(this.service, "momoPartnerCode", PARTNER_CODE);
		ReflectionTestUtils.setField(this.service, "momoAccessKey", ACCESS_KEY);
		ReflectionTestUtils.setField(this.service, "momoSecretKey", SECRET_KEY);
		ReflectionTestUtils.setField(this.service, "momoCreateUrl", "https://test-payment.momo.vn/v2/gateway/api/create");
		ReflectionTestUtils.setField(this.service, "momoRedirectUrl", "https://example.test/api/payments/momo-return");
		ReflectionTestUtils.setField(this.service, "momoIpnUrl", "https://example.test/api/payments/momo-ipn");
	}

	@Test
	void hasPremiumAccessReturnsTrueForUnexpiredSuccessPayment() {
		when(this.userRepository.existsById(1)).thenReturn(true);
		when(this.paymentRepository.findByUserIdAndStatus(1, PaymentStatus.SUCCESS))
				.thenReturn(List.of(successPayment(LocalDateTime.now().minusDays(10))));

		assertTrue(this.service.hasPremiumAccess(1));
	}

	@Test
	void hasPremiumAccessReturnsFalseForExpiredSuccessPayment() {
		when(this.userRepository.existsById(1)).thenReturn(true);
		when(this.paymentRepository.findByUserIdAndStatus(1, PaymentStatus.SUCCESS))
				.thenReturn(List.of(successPayment(LocalDateTime.now().minusDays(31))));

		assertFalse(this.service.hasPremiumAccess(1));
	}

	@Test
	void processMomoCallbackRejectsInvalidSignatureWithoutUpdatingPayment() {
		Map<String, String> params = validCallbackParams(ORDER_ID, "0", "49000", PARTNER_CODE);
		params.put("signature", "invalid");

		assertThrows(RuntimeException.class, () -> this.service.processMomoCallback(params));

		verify(this.paymentRepository, never()).save(ArgumentMatchers.any(Payment.class));
	}

	@Test
	void createPremiumPaymentUsesMomoAtmRequestAndReturnsPayUrl() {
		User user = user();
		when(this.userRepository.findById(1)).thenReturn(Optional.of(user));
		when(this.paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
			Payment payment = invocation.getArgument(0);
			payment.setId(10);
			return payment;
		});
		this.momoCreateResponse = new PaymentServiceImpl.MomoCreateResponse(PARTNER_CODE, ORDER_ID, ORDER_ID, 49000,
				1700000000000L, "Successful.", 0, "https://momo.test/pay", null, "signature");

		CreatePaymentResponse response = this.service.createPremiumPayment(1);

		assertEquals(10, response.paymentId());
		assertEquals("https://momo.test/pay", response.paymentUrl());
		assertEquals(PaymentStatus.PENDING.name(), response.status());
		assertEquals("payWithATM", this.momoCreateRequest.requestType());
		assertEquals(49000L, this.momoCreateRequest.amount());
		assertEquals(PARTNER_CODE, this.momoCreateRequest.partnerCode());
		assertTrue(this.momoCreateRequest.orderId().startsWith("MOMOPREM"));
		assertEquals(this.momoCreateRequest.orderId(), this.momoCreateRequest.requestId());
		assertEquals(ReflectionTestUtils.invokeMethod(this.service, "signMomoCreateRequest",
				this.momoCreateRequest.orderId()), this.momoCreateRequest.signature());
	}

	@Test
	void signMomoCreateRequestUsesPayWithAtmFixture() {
		String signature = ReflectionTestUtils.invokeMethod(this.service, "signMomoCreateRequest", ORDER_ID);

		assertEquals("b03edcfb9455317ff2efb4c3710e748e6815af0a9658c51ca4cb55c931506a92", signature);
	}

	@Test
	void createPremiumPaymentRequiresPayUrlOnly() {
		User user = user();
		when(this.userRepository.findById(1)).thenReturn(Optional.of(user));
		when(this.paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
			Payment payment = invocation.getArgument(0);
			payment.setId(10);
			return payment;
		});
		this.momoCreateResponse = new PaymentServiceImpl.MomoCreateResponse(PARTNER_CODE, ORDER_ID, ORDER_ID, 49000,
				1700000000000L, "Successful.", 0, "https://momo.test/pay", null, "signature");

		CreatePaymentResponse response = this.service.createPremiumPayment(1);

		assertEquals("https://momo.test/pay", response.paymentUrl());
		assertEquals(PaymentStatus.PENDING.name(), response.status());
	}

	@Test
	void processMomoCallbackMarksPendingPaymentSuccess() {
		Payment payment = pendingPayment(ORDER_ID);
		when(this.paymentRepository.findByTransactionId(ORDER_ID)).thenReturn(Optional.of(payment));
		when(this.paymentRepository.save(payment)).thenReturn(payment);
		Map<String, String> params = signedCallbackParams(ORDER_ID, "0", "49000", PARTNER_CODE);

		this.service.processMomoCallback(params);

		assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
		assertNotNull(payment.getCompletedAt());
		verify(this.paymentRepository).save(payment);
	}

	@Test
	void processMomoCallbackMarksFailedTransactionFailed() {
		Payment payment = pendingPayment(ORDER_ID);
		when(this.paymentRepository.findByTransactionId(ORDER_ID)).thenReturn(Optional.of(payment));
		when(this.paymentRepository.save(payment)).thenReturn(payment);
		Map<String, String> params = signedCallbackParams(ORDER_ID, "1006", "49000", PARTNER_CODE);

		this.service.processMomoCallback(params);

		assertEquals(PaymentStatus.FAILED, payment.getStatus());
		verify(this.paymentRepository).save(payment);
	}

	@Test
	void processMomoCallbackRejectsAmountMismatch() {
		Payment payment = pendingPayment(ORDER_ID);
		when(this.paymentRepository.findByTransactionId(ORDER_ID)).thenReturn(Optional.of(payment));
		Map<String, String> params = signedCallbackParams(ORDER_ID, "0", "50000", PARTNER_CODE);

		assertThrows(RuntimeException.class, () -> this.service.processMomoCallback(params));

		verify(this.paymentRepository, never()).save(ArgumentMatchers.any(Payment.class));
	}

	@Test
	void processMomoCallbackRejectsWrongPartnerCode() {
		Map<String, String> params = signedCallbackParams(ORDER_ID, "0", "49000", "OTHER");

		assertThrows(RuntimeException.class, () -> this.service.processMomoCallback(params));

		verify(this.paymentRepository, never()).save(ArgumentMatchers.any(Payment.class));
	}

	@Test
	void processMomoCallbackIsIdempotentForSuccessfulPayment() {
		Payment payment = pendingPayment(ORDER_ID);
		LocalDateTime completedAt = LocalDateTime.now().minusMinutes(1);
		payment.setStatus(PaymentStatus.SUCCESS);
		payment.setCompletedAt(completedAt);
		when(this.paymentRepository.findByTransactionId(ORDER_ID)).thenReturn(Optional.of(payment));
		Map<String, String> params = signedCallbackParams(ORDER_ID, "0", "49000", PARTNER_CODE);

		this.service.processMomoCallback(params);

		assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
		assertEquals(completedAt, payment.getCompletedAt());
		verify(this.paymentRepository, never()).save(payment);
	}

	@Test
	void hmacSha256UsesFixedMoMoSignatureFixture() {
		String rawSignature = "accessKey=test-access&amount=49000&extraData=&message=Successful.&orderId=MOMOPREM123"
				+ "&orderInfo=Thanh toan Premium IELTS AI Practice App&orderType=momo_wallet&partnerCode=MOMO"
				+ "&payType=napas&requestId=MOMOPREM123&responseTime=1700000000000&resultCode=0&transId=123456789";

		String signature = ReflectionTestUtils.invokeMethod(this.service, "hmacSha256", rawSignature);

		assertEquals("7db458d10c55ca640c5eb54ddad9401361038de58742f8b107e776bf34953b53", signature);
	}

	private Payment successPayment(LocalDateTime completedAt) {
		Payment payment = pendingPayment(ORDER_ID);
		payment.setStatus(PaymentStatus.SUCCESS);
		payment.setCompletedAt(completedAt);
		return payment;
	}

	private Payment pendingPayment(String transactionId) {
		Payment payment = new Payment();
		payment.setId(10);
		payment.setUser(user());
		payment.setAmount(new BigDecimal("49000.00"));
		payment.setPaymentMethod(PaymentMethod.MOMO);
		payment.setTransactionId(transactionId);
		payment.setStatus(PaymentStatus.PENDING);
		payment.setPremiumDuration(30);
		payment.setCreatedAt(LocalDateTime.now());
		return payment;
	}

	private User user() {
		User user = new User();
		user.setId(1);
		return user;
	}

	private Map<String, String> signedCallbackParams(String orderId, String resultCode, String amount,
			String partnerCode) {
		Map<String, String> params = validCallbackParams(orderId, resultCode, amount, partnerCode);
		params.put("signature", hmacSha256(rawCallbackSignature(params)));
		return params;
	}

	private Map<String, String> validCallbackParams(String orderId, String resultCode, String amount,
			String partnerCode) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("partnerCode", partnerCode);
		params.put("orderId", orderId);
		params.put("requestId", orderId);
		params.put("amount", amount);
		params.put("orderInfo", "Thanh toan Premium IELTS AI Practice App");
		params.put("orderType", "momo_wallet");
		params.put("transId", "123456789");
		params.put("resultCode", resultCode);
		params.put("message", "Successful.");
		params.put("payType", "napas");
		params.put("responseTime", "1700000000000");
		params.put("extraData", "");
		return params;
	}

	private String rawCallbackSignature(Map<String, String> params) {
		return "accessKey=" + ACCESS_KEY
				+ "&amount=" + params.get("amount")
				+ "&extraData=" + params.get("extraData")
				+ "&message=" + params.get("message")
				+ "&orderId=" + params.get("orderId")
				+ "&orderInfo=" + params.get("orderInfo")
				+ "&orderType=" + params.get("orderType")
				+ "&partnerCode=" + params.get("partnerCode")
				+ "&payType=" + params.get("payType")
				+ "&requestId=" + params.get("requestId")
				+ "&responseTime=" + params.get("responseTime")
				+ "&resultCode=" + params.get("resultCode")
				+ "&transId=" + params.get("transId");
	}

	private String hmacSha256(String data) {
		try {
			Mac hmac = Mac.getInstance("HmacSHA256");
			hmac.init(new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
			StringBuilder result = new StringBuilder(bytes.length * 2);
			for (byte item : bytes) {
				result.append(String.format("%02x", item));
			}
			return result.toString();
		} catch (Exception error) {
			throw new IllegalStateException(error);
		}
	}
}
