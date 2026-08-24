import 'package:flutter/material.dart';

import '../../models/mock_session.dart';
import '../../services/api_service.dart';
import '../../services/mock_session_service.dart';
import '../../widgets/state_views.dart';

class SessionListScreen extends StatefulWidget {
  const SessionListScreen({super.key});

  @override
  State<SessionListScreen> createState() => _SessionListScreenState();
}

class _SessionListScreenState extends State<SessionListScreen> {
  final _mockSessionService = MockSessionService();

  bool _isLoading = true;
  String? _errorMessage;
  List<MockSession> _sessions = [];
  final Set<int> _registeringSessionIds = {};

  @override
  void initState() {
    super.initState();
    _loadSessions();
  }

  Future<void> _loadSessions() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final sessions = await _mockSessionService.getAvailableSessions();
      if (!mounted) return;
      setState(() {
        _sessions = sessions;
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

  Future<void> _register(MockSession session) async {
    setState(() {
      _registeringSessionIds.add(session.id);
    });

    try {
      final registration = await _mockSessionService.registerSession(
        session.id,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Đăng ký thành công. Số báo danh: ${registration.candidateNumber}',
          ),
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
          _registeringSessionIds.remove(session.id);
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Ca thi Premium')),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const LoadingView(message: 'Đang tải ca thi Premium...');
    }

    if (_errorMessage != null) {
      return ErrorView(message: _errorMessage!, onRetry: _loadSessions);
    }

    if (_sessions.isEmpty) {
      return const EmptyState(
        icon: Icons.event_busy,
        message: 'Chưa có ca thi Premium khả dụng.',
      );
    }

    return RefreshIndicator(
      onRefresh: _loadSessions,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _sessions.length,
        separatorBuilder: (context, index) => const SizedBox(height: 12),
        itemBuilder: (context, index) {
          final session = _sessions[index];
          final isRegistering = _registeringSessionIds.contains(session.id);
          return Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    session.examTitle,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 8),
                  Chip(label: Text(_statusLabel(session.status))),
                  const SizedBox(height: 6),
                  Text('Phòng: ${session.roomCode}'),
                  Text('Bắt đầu: ${formatDateTime(session.startTime)}'),
                  Text(
                    'Hạn đăng ký: ${formatDateTime(session.registrationDeadline)}',
                  ),
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: isRegistering ? null : () => _register(session),
                    child: isRegistering
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('Đăng ký'),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

String _statusLabel(String status) {
  switch (status) {
    case 'PENDING':
      return 'Sắp diễn ra';
    case 'ONGOING':
      return 'Đang thi';
    case 'COMPLETED':
      return 'Đã kết thúc';
    default:
      return status;
  }
}
