import 'package:flutter/material.dart';

import '../../services/api_service.dart';
import '../../services/payment_service.dart';
import 'session_list_screen.dart';

class PremiumIntroScreen extends StatefulWidget {
  const PremiumIntroScreen({super.key});

  @override
  State<PremiumIntroScreen> createState() => _PremiumIntroScreenState();
}

class _PremiumIntroScreenState extends State<PremiumIntroScreen> {
  final _paymentService = PaymentService();
  bool _isLoading = false;

  Future<void> _confirmPayment() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Xác nhận thanh toán 49.000đ?'),
        content: const Text('Đây là thanh toán giả lập phục vụ demo đồ án.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Thanh toán'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      _pay();
    }
  }

  Future<void> _pay() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final message = await _paymentService.fakePremiumPayment();
      if (!mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(message)));
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const SessionListScreen()),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('IELTS Premium')),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Text(
            'IELTS Premium',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 16),
          const Card(
            child: Padding(
              padding: EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Quyền lợi'),
                  SizedBox(height: 12),
                  Text('✓ Thi thử IELTS theo ca'),
                  Text('✓ Nhận số báo danh'),
                  Text('✓ Thi cùng thời gian với các thí sinh khác'),
                  Text('✓ Speaking được Expert chấm'),
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),
          Text('49.000đ', style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 4),
          const Text('Thanh toán demo phục vụ đồ án'),
          const SizedBox(height: 20),
          FilledButton(
            onPressed: _isLoading ? null : _confirmPayment,
            child: _isLoading
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Thanh toán để kích hoạt'),
          ),
        ],
      ),
    );
  }
}
