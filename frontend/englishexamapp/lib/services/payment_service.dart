import 'api_service.dart';

class PremiumStatus {
  final bool premium;
  final String expiresAt;
  final String message;

  PremiumStatus({
    required this.premium,
    required this.expiresAt,
    required this.message,
  });

  factory PremiumStatus.fromJson(Map<String, dynamic> json) {
    return PremiumStatus(
      premium: (json['premium'] as bool?) ?? false,
      expiresAt: (json['expiresAt'] as String?) ?? '',
      message: (json['message'] as String?) ?? '',
    );
  }
}

class PremiumPayment {
  final int paymentId;
  final String paymentUrl;
  final String status;
  final String message;

  PremiumPayment({
    required this.paymentId,
    required this.paymentUrl,
    required this.status,
    required this.message,
  });

  factory PremiumPayment.fromJson(Map<String, dynamic> json) {
    return PremiumPayment(
      paymentId: json['paymentId'] as int,
      paymentUrl: (json['paymentUrl'] as String?) ?? '',
      status: (json['status'] as String?) ?? '',
      message: (json['message'] as String?) ?? '',
    );
  }
}

class PaymentStatusResult {
  final int paymentId;
  final String status;
  final String message;

  PaymentStatusResult({
    required this.paymentId,
    required this.status,
    required this.message,
  });

  factory PaymentStatusResult.fromJson(Map<String, dynamic> json) {
    return PaymentStatusResult(
      paymentId: json['paymentId'] as int,
      status: (json['status'] as String?) ?? '',
      message: (json['message'] as String?) ?? '',
    );
  }
}

class PaymentService {
  Future<PremiumStatus> getPremiumDetails() async {
    final response = await ApiService.dio.get('/api/payments/premium-status');
    return PremiumStatus.fromJson(response.data as Map<String, dynamic>);
  }

  Future<bool> getPremiumStatus() async {
    final details = await getPremiumDetails();
    return details.premium;
  }

  Future<PremiumPayment> createPremiumPayment() async {
    final response = await ApiService.dio.post('/api/payments/premium');
    return PremiumPayment.fromJson(response.data as Map<String, dynamic>);
  }

  Future<PaymentStatusResult> getPaymentStatus(int paymentId) async {
    final response = await ApiService.dio.get(
      '/api/payments/$paymentId/status',
    );
    return PaymentStatusResult.fromJson(response.data as Map<String, dynamic>);
  }

  Future<String> fakePremiumPayment() async {
    final response = await ApiService.dio.post('/api/payments/fake-premium');
    final data = response.data as Map<String, dynamic>;
    return (data['message'] as String?) ?? 'Thanh toán thành công.';
  }
}
