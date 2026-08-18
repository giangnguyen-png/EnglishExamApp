import 'package:flutter/material.dart';

import '../../models/result.dart';
import '../../services/api_service.dart';
import '../../services/attempt_service.dart';
import '../result/result_screen.dart';

class AttemptHistoryScreen extends StatefulWidget {
  const AttemptHistoryScreen({super.key});

  @override
  State<AttemptHistoryScreen> createState() => _AttemptHistoryScreenState();
}

class _AttemptHistoryScreenState extends State<AttemptHistoryScreen> {
  final _attemptService = AttemptService();

  bool _isLoading = true;
  String? _errorMessage;
  List<AttemptHistory> _attempts = [];

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  Future<void> _loadHistory() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final attempts = await _attemptService.getAttemptHistory();
      if (!mounted) return;
      setState(() {
        _attempts = attempts;
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

  Future<void> _openResult(AttemptHistory attempt) async {
    try {
      final result = await _attemptService.getAttemptResult(attempt.attemptId);
      if (!mounted) return;
      Navigator.push(
        context,
        MaterialPageRoute(builder: (_) => ResultScreen(result: result)),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Lịch sử làm bài')),
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
              Text(
                _errorMessage!,
                textAlign: TextAlign.center,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
              const SizedBox(height: 16),
              OutlinedButton(
                onPressed: _loadHistory,
                child: const Text('Thử lại'),
              ),
            ],
          ),
        ),
      );
    }

    if (_attempts.isEmpty) {
      return const Center(child: Text('Bạn chưa có lịch sử làm bài.'));
    }

    return RefreshIndicator(
      onRefresh: _loadHistory,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _attempts.length,
        separatorBuilder: (context, index) => const SizedBox(height: 12),
        itemBuilder: (context, index) {
          final attempt = _attempts[index];
          return Card(
            child: ListTile(
              title: Text(attempt.examTitle),
              subtitle: Text(
                _formatDate(
                  attempt.endTime.isEmpty ? attempt.startTime : attempt.endTime,
                ),
              ),
              trailing: Text(_formatBand(attempt.overallBandScore)),
              onTap: () => _openResult(attempt),
            ),
          );
        },
      ),
    );
  }

  String _formatDate(String raw) {
    if (raw.isEmpty) {
      return 'Chưa có ngày';
    }

    final date = DateTime.tryParse(raw);
    if (date == null) {
      return raw;
    }

    return '${date.day.toString().padLeft(2, '0')}/'
        '${date.month.toString().padLeft(2, '0')}/${date.year}';
  }

  String _formatBand(double? value) {
    if (value == null) {
      return 'Chưa có điểm';
    }
    return 'Band ${value.toStringAsFixed(1)}';
  }
}
