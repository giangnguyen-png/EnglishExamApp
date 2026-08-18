import 'api_service.dart';

class PaymentService {
  Future<bool> getPremiumStatus() async {
    final response = await ApiService.dio.get('/api/payments/premium-status');
    final data = response.data as Map<String, dynamic>;
    return (data['premium'] as bool?) ?? false;
  }

  Future<String> fakePremiumPayment() async {
    final response = await ApiService.dio.post('/api/payments/fake-premium');
    final data = response.data as Map<String, dynamic>;
    return (data['message'] as String?) ?? 'Thanh toán thành công.';
  }
}
