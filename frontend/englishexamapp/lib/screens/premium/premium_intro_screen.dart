import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../config/app_colors.dart';
import '../../services/api_service.dart';
import '../../services/payment_service.dart';
import '../../widgets/accent_card.dart';
import 'session_list_screen.dart';

class PremiumIntroScreen extends StatefulWidget {
  const PremiumIntroScreen({super.key});

  @override
  State<PremiumIntroScreen> createState() => _PremiumIntroScreenState();
}

class _PremiumIntroScreenState extends State<PremiumIntroScreen> {
  final _paymentService = PaymentService();
  bool _isLoading = true;
  bool _isCreatingPayment = false;
  bool _isCheckingPayment = false;
  PremiumStatus? _premiumStatus;
  PremiumPayment? _pendingPayment;

  @override
  void initState() {
    super.initState();
    _loadPremiumStatus();
  }

  Future<void> _loadPremiumStatus() async {
    setState(() {
      _isLoading = true;
    });
    try {
      final status = await _paymentService.getPremiumDetails();
      if (!mounted) return;
      setState(() {
        _premiumStatus = status;
      });
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

  Future<void> _createPayment() async {
    setState(() {
      _isCreatingPayment = true;
    });

    try {
      final payment = await _paymentService.createPremiumPayment();
      if (!mounted) return;
      setState(() {
        _pendingPayment = payment;
      });
      if (payment.status == 'SUCCESS') {
        await _loadPremiumStatus();
        return;
      }
      await _openPaymentUrl(payment);
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isCreatingPayment = false;
        });
      }
    }
  }

  Future<void> _openPaymentUrl(PremiumPayment payment) async {
    if (payment.paymentUrl.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể mở MoMo Sandbox.')),
      );
      return;
    }
    final launched = await launchUrl(
      Uri.parse(payment.paymentUrl),
      mode: LaunchMode.externalApplication,
    );
    if (!mounted) return;
    if (!launched) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể mở MoMo Sandbox.')),
      );
    }
  }

  Future<void> _checkPaymentStatus() async {
    final payment = _pendingPayment;
    if (payment == null) {
      return;
    }
    setState(() {
      _isCheckingPayment = true;
    });
    try {
      final status = await _paymentService.getPaymentStatus(payment.paymentId);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(_paymentStatusMessage(status.status))),
      );
      if (status.status == 'SUCCESS') {
        await _loadPremiumStatus();
      }
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isCheckingPayment = false;
        });
      }
    }
  }

  void _openSessions() {
    Navigator.pushReplacement(
      context,
      MaterialPageRoute(builder: (_) => const SessionListScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final status = _premiumStatus;
    final isPremium = status?.premium ?? false;

    return Scaffold(
      appBar: AppBar(title: const Text('IELTS Premium')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(24),
              children: [
                Icon(
                  Icons.workspace_premium,
                  size: 64,
                  color: AppColors.premium,
                ),
                const SizedBox(height: 16),
                Text(
                  'Premium 30 ngày',
                  style: Theme.of(context).textTheme.headlineSmall,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                Text(
                  isPremium
                      ? 'Premium đang hoạt động'
                      : 'Thanh toán thử nghiệm qua MoMo Sandbox bằng thẻ ATM test',
                  textAlign: TextAlign.center,
                ),
                if (isPremium && status!.expiresAt.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Text(
                    'Hết hạn: ${_formatBackendDate(status.expiresAt)}',
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ],
                const SizedBox(height: 16),
                const AccentCard(
                  color: AppColors.premium,
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Quyền lợi'),
                        SizedBox(height: 12),
                        Text('Thi thử IELTS theo ca'),
                        Text('Nhận số báo danh'),
                        Text('Thi cùng thời gian với các thí sinh khác'),
                        Text('Speaking được Expert chấm'),
                      ],
                  ),
                ),
                const SizedBox(height: 20),
                if (!isPremium) ...[
                  Text(
                    '49.000đ',
                    style: Theme.of(context)
                        .textTheme
                        .headlineMedium
                        ?.copyWith(
                          color: AppColors.premium,
                          fontWeight: FontWeight.w700,
                        ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    'Giao dịch Sandbox, không sử dụng tiền thật',
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 20),
                  FilledButton(
                    onPressed: _isCreatingPayment ? null : _createPayment,
                    child: _isCreatingPayment
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('Thanh toán bằng thẻ ATM'),
                  ),
                  if (_pendingPayment != null) ...[
                    const SizedBox(height: 12),
                    OutlinedButton(
                      onPressed: _isCheckingPayment ? null : _checkPaymentStatus,
                      child: _isCheckingPayment
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Text('Kiểm tra trạng thái thanh toán'),
                    ),
                  ],
                ] else ...[
                  FilledButton(
                    onPressed: _openSessions,
                    child: const Text('Vào ca thi Premium'),
                  ),
                ],
              ],
            ),
    );
  }

  String _formatBackendDate(String value) {
    final date = DateTime.tryParse(value);
    if (date == null) {
      return value;
    }
    final day = date.day.toString().padLeft(2, '0');
    final month = date.month.toString().padLeft(2, '0');
    return '$day/$month/${date.year}';
  }

  String _paymentStatusMessage(String status) {
    if (status == 'SUCCESS') {
      return 'Thanh toán thành công.\nPremium đã được kích hoạt.';
    }
    if (status == 'FAILED') {
      return 'Giao dịch thất bại.';
    }
    return 'Giao dịch đang được xử lý.\nVui lòng thử kiểm tra lại sau.';
  }
}
