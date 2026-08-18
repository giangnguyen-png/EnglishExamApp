import 'package:flutter/material.dart';

import '../../models/mock_session.dart';
import '../../services/api_service.dart';
import '../../services/expert_service.dart';
import 'expert_session_detail_screen.dart';
import 'session_form_screen.dart';

class ExpertSessionListScreen extends StatefulWidget {
  const ExpertSessionListScreen({super.key});

  @override
  State<ExpertSessionListScreen> createState() =>
      _ExpertSessionListScreenState();
}

class _ExpertSessionListScreenState extends State<ExpertSessionListScreen> {
  final _expertService = ExpertService();

  bool _isLoading = true;
  String? _errorMessage;
  List<MockSession> _sessions = [];

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
      final sessions = await _expertService.getMySessions();
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

  Future<void> _openForm() async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const SessionFormScreen()),
    );
    _loadSessions();
  }

  Future<void> _openDetail(MockSession session) async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => ExpertSessionDetailScreen(sessionId: session.id),
      ),
    );
    _loadSessions();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Ca thi của tôi')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openForm,
        icon: const Icon(Icons.add),
        label: const Text('Tạo ca thi'),
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) return const Center(child: CircularProgressIndicator());
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
    if (_sessions.isEmpty) return const Center(child: Text('Chưa có ca thi.'));

    return RefreshIndicator(
      onRefresh: _loadSessions,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _sessions.length,
        separatorBuilder: (context, index) => const SizedBox(height: 12),
        itemBuilder: (context, index) {
          final session = _sessions[index];
          return Card(
            child: ListTile(
              title: Text(session.roomCode),
              subtitle: Text(
                '${session.examTitle}\n'
                '${_formatDateTime(session.startTime)}\n'
                '${session.status}',
              ),
              isThreeLine: true,
              trailing: const Icon(Icons.chevron_right),
              onTap: () => _openDetail(session),
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
