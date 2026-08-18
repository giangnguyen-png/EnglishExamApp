import 'package:flutter/material.dart';

import '../../models/mock_session.dart';
import '../../services/api_service.dart';
import '../../services/expert_service.dart';
import 'speaking_grading_screen.dart';

class SpeakingAttemptListScreen extends StatefulWidget {
  final int sessionId;

  const SpeakingAttemptListScreen({super.key, required this.sessionId});

  @override
  State<SpeakingAttemptListScreen> createState() =>
      _SpeakingAttemptListScreenState();
}

class _SpeakingAttemptListScreenState extends State<SpeakingAttemptListScreen> {
  final _expertService = ExpertService();
  bool _isLoading = true;
  String? _errorMessage;
  List<SpeakingAttempt> _attempts = [];

  @override
  void initState() {
    super.initState();
    _loadAttempts();
  }

  Future<void> _loadAttempts() async {
    try {
      final attempts = await _expertService.getSpeakingAttempts(
        widget.sessionId,
      );
      if (!mounted) return;
      setState(() {
        _attempts = attempts;
        _errorMessage = null;
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Speaking Attempts')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
          ? Center(child: Text(_errorMessage!))
          : _attempts.isEmpty
          ? const Center(child: Text('Chưa có bài Speaking.'))
          : ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: _attempts.length,
              separatorBuilder: (context, index) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                final attempt = _attempts[index];
                return Card(
                  child: ListTile(
                    title: Text('SBD ${attempt.candidateNumber}'),
                    subtitle: Text(attempt.username),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => SpeakingGradingScreen(attempt: attempt),
                      ),
                    ),
                  ),
                );
              },
            ),
    );
  }
}
