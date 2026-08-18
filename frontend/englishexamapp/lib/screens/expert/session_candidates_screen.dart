import 'package:flutter/material.dart';

import '../../models/mock_session.dart';
import '../../services/api_service.dart';
import '../../services/expert_service.dart';

class SessionCandidatesScreen extends StatefulWidget {
  final int sessionId;

  const SessionCandidatesScreen({super.key, required this.sessionId});

  @override
  State<SessionCandidatesScreen> createState() =>
      _SessionCandidatesScreenState();
}

class _SessionCandidatesScreenState extends State<SessionCandidatesScreen> {
  final _expertService = ExpertService();
  bool _isLoading = true;
  String? _errorMessage;
  List<ExpertRegistration> _registrations = [];

  @override
  void initState() {
    super.initState();
    _loadRegistrations();
  }

  Future<void> _loadRegistrations() async {
    try {
      final registrations = await _expertService.getRegistrations(
        widget.sessionId,
      );
      if (!mounted) return;
      setState(() {
        _registrations = registrations;
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
      appBar: AppBar(title: const Text('Danh sách thí sinh')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
          ? Center(child: Text(_errorMessage!))
          : _registrations.isEmpty
          ? const Center(child: Text('Chưa có thí sinh.'))
          : ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: _registrations.length,
              separatorBuilder: (context, index) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                final registration = _registrations[index];
                return Card(
                  child: ListTile(
                    title: Text('SBD ${registration.candidateNumber}'),
                    subtitle: Text(registration.username),
                  ),
                );
              },
            ),
    );
  }
}
