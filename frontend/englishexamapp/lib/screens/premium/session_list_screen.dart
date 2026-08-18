import 'package:flutter/material.dart';

import '../../models/mock_session.dart';
import '../../services/api_service.dart';
import '../../services/mock_session_service.dart';

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
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(_errorMessage!, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              OutlinedButton(
                onPressed: _loadSessions,
                child: const Text('Thử lại'),
              ),
            ],
          ),
        ),
      );
    }

    if (_sessions.isEmpty) {
      return const Center(child: Text('Chưa có ca thi phù hợp.'));
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
                  const SizedBox(height: 6),
                  Text('Phòng: ${session.roomCode}'),
                  Text('Bắt đầu: ${_formatDateTime(session.startTime)}'),
                  Text(
                    'Hạn đăng ký: ${_formatDateTime(session.registrationDeadline)}',
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

String _formatDateTime(String raw) {
  final date = DateTime.tryParse(raw);
  if (date == null) return raw.isEmpty ? 'Chưa có' : raw;
  return '${date.day.toString().padLeft(2, '0')}/'
      '${date.month.toString().padLeft(2, '0')}/${date.year} '
      '${date.hour.toString().padLeft(2, '0')}:'
      '${date.minute.toString().padLeft(2, '0')}';
}
