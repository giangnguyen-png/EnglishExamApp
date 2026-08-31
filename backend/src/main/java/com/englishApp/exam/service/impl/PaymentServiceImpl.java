package com.englishApp.exam.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.englishApp.exam.dto.payment.CreatePaymentResponse;
import com.englishApp.exam.model.Payment;
import com.englishApp.exam.model.User;
import com.englishApp.exam.model.enums.PaymentMethod;
import com.englishApp.exam.model.enums.PaymentStatus;
import com.englishApp.exam.repository.PaymentRepository;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.service.PaymentService;

@Service
public class PaymentServiceImpl implements PaymentService {
	private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
	private static final BigDecimal PREMIUM_AMOUNT = new BigDecimal("49000.00");
	private static final long MOMO_AMOUNT = 49000L;
	private static final int PREMIUM_DURATION_DAYS = 30;
	private static final String MOMO_REQUEST_TYPE = "payWithATM";
	private static final String MOMO_ORDER_INFO = "Thanh toan Premium IELTS AI Practice App";
	private static final String MOMO_EXTRA_DATA = "";
	private static final String MOMO_LANG = "vi";

	@Value("${payment.mock-enabled:false}")
	private boolean paymentMockEnabled;
	@Value("${momo.partner-code:MOMO}")
	private String momoPartnerCode;
	@Value("${momo.access-key:}")
	private String momoAccessKey;
	@Value("${momo.secret-key:}")
	private String momoSecretKey;
	@Value("${momo.create-url:https://test-payment.momo.vn/v2/gateway/api/create}")
	private String momoCreateUrl;
	@Value("${momo.redirect-url:}")
	private String momoRedirectUrl;
	@Value("${momo.ipn-url:}")
	private String momoIpnUrl;

	private final PaymentRepository paymentRepository;
	private final UserRepository userRepository;
	private final RestClient restClient;

	@Autowired
	public PaymentServiceImpl(PaymentRepository paymentRepository, UserRepository userRepository) {
		this(paymentRepository, userRepository, RestClient.create());
	}

	PaymentServiceImpl(PaymentRepository paymentRepository, UserRepository userRepository, RestClient restClient) {
		this.paymentRepository = paymentRepository;
		this.userRepository = userRepository;
		this.restClient = restClient;
	}

	public CreatePaymentResponse createPremiumPayment(Integer userId) {
		if (this.paymentMockEnabled) {
			Payment payment = createFakePremiumPaymentInternal(userId);
			return new CreatePaymentResponse(payment.getId(), "", payment.getStatus().name(),
					"Thanh toán mock thành công. Tài khoản Premium đã được kích hoạt.");
		}
		validateMomoConfig();
		User user = this.userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		Payment payment = new Payment();
		payment.setUser(user);
		payment.setAmount(PREMIUM_AMOUNT);
		payment.setPaymentMethod(PaymentMethod.MOMO);
		payment.setTransactionId(newTransactionId());
		payment.setStatus(PaymentStatus.PENDING);
		payment.setPremiumDuration(PREMIUM_DURATION_DAYS);
		Payment savedPayment = this.paymentRepository.save(payment);
		MomoCreateResponse momoResponse = createMomoPayment(savedPayment);
		return new CreatePaymentResponse(savedPayment.getId(), momoResponse.payUrl(), savedPayment.getStatus().name(),
				"Đã tạo giao dịch MoMo ATM Sandbox.");
	}

	public Payment createFakePremiumPayment(Integer userId) {
		if (!this.paymentMockEnabled) {
			throw new RuntimeException("Mock payment is disabled");
		}
		return createFakePremiumPaymentInternal(userId);
	}

	private Payment createFakePremiumPaymentInternal(Integer userId) {
		User user = this.userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		Payment payment = new Payment();
		payment.setUser(user);
		payment.setAmount(PREMIUM_AMOUNT);
		payment.setPaymentMethod(PaymentMethod.FAKE);
		payment.setTransactionId("DEMO_" + UUID.randomUUID());
		payment.setStatus(PaymentStatus.SUCCESS);
		payment.setPremiumDuration(PREMIUM_DURATION_DAYS);
		payment.setCompletedAt(LocalDateTime.now());
		return this.paymentRepository.save(payment);
	}

	public Payment processMomoCallback(Map<String, String> params) {
		if (!isValidMomoCallbackSignature(params)) {
			log.warn("Invalid MoMo callback signature for orderId={}", params.get("orderId"));
			throw new RuntimeException("Invalid MoMo signature");
		}
		if (!this.momoPartnerCode.equals(params.get("partnerCode"))) {
			log.warn("Invalid MoMo partnerCode for orderId={}", params.get("orderId"));
			throw new RuntimeException("MoMo partnerCode does not match configuration");
		}
		Payment payment = this.findByTransactionId(params.get("orderId"));
		validateCallbackAmount(payment, params.get("amount"));
		if (payment.getStatus() == PaymentStatus.SUCCESS) {
			return payment;
		}
		boolean success = "0".equals(params.get("resultCode"));
		payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
		if (success) {
			payment.setCompletedAt(LocalDateTime.now());
		}
		return this.paymentRepository.save(payment);
	}

	public Payment findById(Integer id) {
		return this.paymentRepository.findById(id).orElseThrow(() -> new RuntimeException("Payment not found"));
	}

	private Payment findByTransactionId(String transactionId) {
		return this.paymentRepository.findByTransactionId(transactionId)
				.orElseThrow(() -> new RuntimeException("Payment not found"));
	}

	public List<Payment> findByUser(Integer userId) {
		if (!this.userRepository.existsById(userId)) {
			throw new RuntimeException("User not found");
		}
		return this.paymentRepository.findByUserId(userId);
	}

	public boolean hasPremiumAccess(Integer userId) {
		return findPremiumExpiresAt(userId) != null;
	}

	public LocalDateTime findPremiumExpiresAt(Integer userId) {
		if (!this.userRepository.existsById(userId)) {
			throw new RuntimeException("User not found");
		}
		LocalDateTime now = LocalDateTime.now();
		return this.paymentRepository.findByUserIdAndStatus(userId, PaymentStatus.SUCCESS).stream()
				.map(this::premiumExpiry)
				.filter(expiry -> expiry != null && expiry.isAfter(now))
				.max(Comparator.naturalOrder())
				.orElse(null);
	}

	public void requirePremium(Integer userId) {
		if (!hasPremiumAccess(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Premium membership required");
		}
	}

	private MomoCreateResponse createMomoPayment(Payment payment) {
		String orderId = payment.getTransactionId();
		MomoCreateRequest request = new MomoCreateRequest(this.momoPartnerCode, orderId, MOMO_AMOUNT, orderId,
				MOMO_ORDER_INFO, this.momoRedirectUrl, this.momoIpnUrl, MOMO_REQUEST_TYPE, MOMO_EXTRA_DATA, MOMO_LANG,
				signMomoCreateRequest(orderId));
		try {
			MomoCreateResponse response = postMomoCreateRequest(request);
			if (response != null && Integer.valueOf(0).equals(response.resultCode()) && response.payUrl() != null
					&& !response.payUrl().isBlank()) {
				return response;
			}
			String message = response == null ? "No response from MoMo" : response.message();
			Integer resultCode = response == null ? null : response.resultCode();
			markPaymentFailed(payment);
			log.warn("MoMo create payment failed orderId={} resultCode={} message={}", orderId, resultCode, message);
			throw new RuntimeException("Không thể tạo giao dịch MoMo Sandbox: " + message);
		} catch (RuntimeException error) {
			markPaymentFailed(payment);
			log.warn("MoMo create payment failed orderId={} message={}", orderId, error.getMessage());
			throw error;
		}
	}

	private void markPaymentFailed(Payment payment) {
		if (payment.getStatus() != PaymentStatus.FAILED) {
			payment.setStatus(PaymentStatus.FAILED);
			this.paymentRepository.save(payment);
		}
	}

	MomoCreateResponse postMomoCreateRequest(MomoCreateRequest request) {
		return this.restClient.post()
				.uri(this.momoCreateUrl)
				.body(request)
				.retrieve()
				.body(MomoCreateResponse.class);
	}

	private String signMomoCreateRequest(String orderId) {
		String rawSignature = "accessKey=" + this.momoAccessKey
				+ "&amount=" + MOMO_AMOUNT
				+ "&extraData=" + MOMO_EXTRA_DATA
				+ "&ipnUrl=" + this.momoIpnUrl
				+ "&orderId=" + orderId
				+ "&orderInfo=" + MOMO_ORDER_INFO
				+ "&partnerCode=" + this.momoPartnerCode
				+ "&redirectUrl=" + this.momoRedirectUrl
				+ "&requestId=" + orderId
				+ "&requestType=" + MOMO_REQUEST_TYPE;
		return hmacSha256(rawSignature);
	}

	private boolean isValidMomoCallbackSignature(Map<String, String> params) {
		String signature = params.get("signature");
		if (signature == null || signature.isBlank()) {
			return false;
		}
		String rawSignature = "accessKey=" + this.momoAccessKey
				+ "&amount=" + value(params, "amount")
				+ "&extraData=" + value(params, "extraData")
				+ "&message=" + value(params, "message")
				+ "&orderId=" + value(params, "orderId")
				+ "&orderInfo=" + value(params, "orderInfo")
				+ "&orderType=" + value(params, "orderType")
				+ "&partnerCode=" + value(params, "partnerCode")
				+ "&payType=" + value(params, "payType")
				+ "&requestId=" + value(params, "requestId")
				+ "&responseTime=" + value(params, "responseTime")
				+ "&resultCode=" + value(params, "resultCode")
				+ "&transId=" + value(params, "transId");
		return signature.equalsIgnoreCase(hmacSha256(rawSignature));
	}

	private String value(Map<String, String> params, String key) {
		String value = params.get(key);
		return value == null ? "" : value;
	}

	private String hmacSha256(String data) {
		try {
			Mac hmac = Mac.getInstance("HmacSHA256");
			hmac.init(new SecretKeySpec(this.momoSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
			StringBuilder result = new StringBuilder(bytes.length * 2);
			for (byte item : bytes) {
				result.append(String.format("%02x", item));
			}
			return result.toString();
		} catch (Exception error) {
			throw new IllegalStateException("Could not sign MoMo data", error);
		}
	}

	private void validateMomoConfig() {
		if (this.momoPartnerCode == null || this.momoPartnerCode.isBlank()
				|| this.momoAccessKey == null || this.momoAccessKey.isBlank()
				|| this.momoSecretKey == null || this.momoSecretKey.isBlank()) {
			throw new RuntimeException("MoMo Sandbox credentials are not configured");
		}
		if (this.momoRedirectUrl == null || this.momoRedirectUrl.isBlank()
				|| this.momoIpnUrl == null || this.momoIpnUrl.isBlank()) {
			throw new RuntimeException("MoMo redirect URL and IPN URL are required when mock payment is disabled");
		}
	}

	private void validateCallbackAmount(Payment payment, String momoAmount) {
		String expectedAmount = payment.getAmount().setScale(0, RoundingMode.HALF_UP).toPlainString();
		if (!expectedAmount.equals(momoAmount)) {
			log.warn("MoMo amount mismatch orderId={} expected={} actual={}", payment.getTransactionId(), expectedAmount,
					momoAmount);
			throw new RuntimeException("MoMo amount does not match payment");
		}
	}

	private LocalDateTime premiumExpiry(Payment payment) {
		Integer duration = payment.getPremiumDuration();
		if (duration == null || duration <= 0) {
			return null;
		}
		LocalDateTime start = payment.getCompletedAt() != null ? payment.getCompletedAt() : payment.getCreatedAt();
		return start == null ? null : start.plusDays(duration);
	}

	private String newTransactionId() {
		return "MOMOPREM" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}

	record MomoCreateRequest(String partnerCode, String requestId, long amount, String orderId,
			String orderInfo, String redirectUrl, String ipnUrl, String requestType, String extraData, String lang,
			String signature) {
	}

	record MomoCreateResponse(String partnerCode, String orderId, String requestId, long amount,
			Long responseTime, String message, Integer resultCode, String payUrl, String deeplink, String signature) {
	}
}
