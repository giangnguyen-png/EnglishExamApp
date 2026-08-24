import 'package:flutter/material.dart';

import '../../models/mock_session.dart';
import '../../services/api_service.dart';
import '../../services/expert_service.dart';
import '../../widgets/state_views.dart';
import 'session_candidates_screen.dart';
import 'session_form_screen.dart';
import 'speaking_attempt_list_screen.dart';

class ExpertSessionDetailScreen extends StatefulWidget {
  final int sessionId;

  const ExpertSessionDetailScreen({super.key, required this.sessionId});

  @override
  State<ExpertSessionDetailScreen> createState() =>
      _ExpertSessionDetailScreenState();
}

class _ExpertSessionDetailScreenState extends State<ExpertSessionDetailScreen> {
  final _expertService = ExpertService();

  bool _isLoading = true;
  bool _isActionLoading = false;
  String? _errorMessage;
  MockSession? _session;

  @override
  void initState() {
    super.initState();
    _loadSession();
  }

  Future<void> _loadSession() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final session = await _expertService.getSessionDetail(widget.sessionId);
      if (!mounted) return;
      setState(() {
        _session = session;
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

  Future<void> _runAction(Future<void> Function() action) async {
    setState(() {
      _isActionLoading = true;
    });
    try {
      await action();
      await _loadSession();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isActionLoading = false;
        });
      }
    }
  }

  Future<void> _deleteSession() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Xóa ca thi?'),
        content: const Text('Chỉ ca PENDING mới có thể xóa.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    await _runAction(() async {
      await _expertService.deleteSession(widget.sessionId);
      if (!mounted) return;
      Navigator.pop(context);
    });
  }

  Future<void> _openForm(MockSession session) async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => SessionFormScreen(session: session)),
    );
    _loadSession();
  }

  @override
  Widget build(BuildContext context) {
    final session = _session;

    return Scaffold(
      appBar: AppBar(title: const Text('Chi tiết ca thi')),
      body: _isLoading
          ? const LoadingView(message: 'Đang tải chi tiết ca thi...')
          : _errorMessage != null
          ? Center(child: Text(_errorMessage!))
          : session == null
          ? const Center(child: Text('Không tìm thấy ca thi.'))
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          session.roomCode,
                          style: Theme.of(context).textTheme.headlineSmall,
                        ),
                        const SizedBox(height: 8),
                        Text(session.examTitle),
                        Text('Bắt đầu: ${formatDateTime(session.startTime)}'),
                        Text('Kết thúc: ${formatDateTime(session.endTime)}'),
                        Text(
                          'Hạn đăng ký: ${formatDateTime(session.registrationDeadline)}',
                        ),
                        Text('Trạng thái: ${_statusLabel(session.status)}'),
                        Text(
                          'Số đăng ký: ${session.registrationCount ?? 0}/${session.maxCandidates}',
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                if (_isActionLoading) const LinearProgressIndicator(),
                if (session.status == 'PENDING') ...[
                  FilledButton(
                    onPressed: _isActionLoading
                        ? null
                        : () => _openForm(session),
                    child: const Text('Chỉnh sửa'),
                  ),
                  const SizedBox(height: 8),
                  FilledButton(
                    onPressed: _isActionLoading
                        ? null
                        : () => _runAction(
                            () => _expertService.startSession(session.id),
                          ),
                    child: const Text('Bắt đầu ca thi'),
                  ),
                  const SizedBox(height: 8),
                  OutlinedButton(
                    onPressed: _isActionLoading ? null : _deleteSession,
                    child: const Text('Xóa'),
                  ),
                ],
                if (session.status == 'ONGOING' ||
                    session.status == 'COMPLETED') ...[
                  FilledButton(
                    onPressed: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) =>
                            SessionCandidatesScreen(sessionId: session.id),
                      ),
                    ),
                    child: const Text('Danh sách thí sinh'),
                  ),
                  const SizedBox(height: 8),
                  FilledButton(
                    onPressed: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) =>
                            SpeakingAttemptListScreen(sessionId: session.id),
                      ),
                    ),
                    child: const Text('Speaking'),
                  ),
                ],
                if (session.status == 'ONGOING') ...[
                  const SizedBox(height: 8),
                  OutlinedButton(
                    onPressed: _isActionLoading
                        ? null
                        : () => _runAction(
                            () => _expertService.finishSession(session.id),
                          ),
                    child: const Text('Kết thúc ca thi'),
                  ),
                ],
              ],
            ),
    );
  }
}

String _statusLabel(String status) {
  switch (status) {
    case 'PENDING':
      return 'Đang chờ';
    case 'ONGOING':
      return 'Đang thi';
    case 'COMPLETED':
      return 'Đã kết thúc';
    default:
      return status;
  }
}
