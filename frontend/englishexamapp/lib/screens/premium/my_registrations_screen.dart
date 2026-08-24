import 'package:flutter/material.dart';

import '../../models/attempt.dart';
import '../../models/exam.dart';
import '../../models/mock_session.dart';
import '../../services/api_service.dart';
import '../../services/attempt_service.dart';
import '../../services/mock_session_service.dart';
import '../../widgets/state_views.dart';
import '../test/test_screen.dart';

class MyRegistrationsScreen extends StatefulWidget {
  const MyRegistrationsScreen({super.key});

  @override
  State<MyRegistrationsScreen> createState() => _MyRegistrationsScreenState();
}

class _MyRegistrationsScreenState extends State<MyRegistrationsScreen> {
  final _mockSessionService = MockSessionService();
  final _attemptService = AttemptService();

  bool _isLoading = true;
  String? _errorMessage;
  List<SessionRegistration> _registrations = [];
  final Set<int> _busyRegistrationIds = {};

  @override
  void initState() {
    super.initState();
    _loadRegistrations();
  }

  Future<void> _loadRegistrations() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final registrations = await _mockSessionService.getMyRegistrations();
      if (!mounted) return;
      setState(() {
        _registrations = registrations;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _errorMessage = ApiService.getErrorMessage(error);
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _cancel(SessionRegistration registration) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Hủy đăng ký?'),
        content: Text('Hủy đăng ký phòng ${registration.roomCode}?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Không'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Hủy đăng ký'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    setState(() {
      _busyRegistrationIds.add(registration.registrationId);
    });

    try {
      await _mockSessionService.cancelRegistration(registration.registrationId);
      await _loadRegistrations();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
    } finally {
      if (mounted) {
        setState(() {
          _busyRegistrationIds.remove(registration.registrationId);
        });
      }
    }
  }

  Future<void> _startAttempt(SessionRegistration registration) async {
    setState(() {
      _busyRegistrationIds.add(registration.registrationId);
    });

    try {
      final Attempt attempt = await _mockSessionService.startPremiumAttempt(
        registration.sessionId,
      );
      final Exam exam = await _attemptService.getAttemptExam(attempt.attemptId);
      if (!mounted) return;
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) =>
              TestScreen(exam: exam, attempt: attempt, listeningMaxPlays: 1),
        ),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
    } finally {
      if (mounted) {
        setState(() {
          _busyRegistrationIds.remove(registration.registrationId);
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Ca thi đã đăng ký')),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const LoadingView(message: 'Đang tải ca thi đã đăng ký...');
    }

    if (_errorMessage != null) {
      return ErrorView(message: _errorMessage!, onRetry: _loadRegistrations);
    }

    if (_registrations.isEmpty) {
      return const EmptyState(
        icon: Icons.badge,
        message: 'Bạn chưa đăng ký ca thi nào.',
      );
    }

    return RefreshIndicator(
      onRefresh: _loadRegistrations,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _registrations.length,
        separatorBuilder: (context, index) => const SizedBox(height: 12),
        itemBuilder: (context, index) {
          final registration = _registrations[index];
          final isBusy = _busyRegistrationIds.contains(
            registration.registrationId,
          );
          return Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    registration.examTitle,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 12),
                  Center(
                    child: Column(
                      children: [
                        const Text('Số báo danh'),
                        Text(
                          registration.candidateNumber.toString().padLeft(
                            2,
                            '0',
                          ),
                          style: Theme.of(context).textTheme.displaySmall,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text('Phòng ${registration.roomCode}'),
                  Text(formatDateTime(registration.startTime)),
                  Text(_statusLabel(registration.status)),
                  const SizedBox(height: 12),
                  _buildAction(registration, isBusy) ?? const SizedBox.shrink(),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget? _buildAction(SessionRegistration registration, bool isBusy) {
    if (isBusy) {
      return const SizedBox(
        width: 24,
        height: 24,
        child: CircularProgressIndicator(strokeWidth: 2),
      );
    }
    if (registration.status == 'PENDING') {
      return OutlinedButton(
        onPressed: () => _cancel(registration),
        child: const Text('Hủy'),
      );
    }
    if (registration.status == 'ONGOING') {
      return FilledButton(
        onPressed: () => _startAttempt(registration),
        child: const Text('Vào thi'),
      );
    }
    return null;
  }
}

String _statusLabel(String status) {
  switch (status) {
    case 'PENDING':
      return 'Trạng thái: Sắp diễn ra';
    case 'ONGOING':
      return 'Trạng thái: Đang thi';
    case 'COMPLETED':
      return 'Trạng thái: Đã kết thúc';
    default:
      return 'Trạng thái: $status';
  }
}
