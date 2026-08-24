import 'package:flutter/material.dart';

import '../../models/result.dart';
import '../../services/api_service.dart';
import '../../services/attempt_service.dart';
import '../../widgets/state_views.dart';
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
      return const LoadingView(message: 'Đang tải lịch sử làm bài...');
    }

    if (_errorMessage != null) {
      return ErrorView(message: _errorMessage!, onRetry: _loadHistory);
    }

    if (_attempts.isEmpty) {
      return const EmptyState(
        icon: Icons.history,
        message: 'Bạn chưa có bài thi trong lịch sử.',
      );
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
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    attempt.examTitle,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 6),
                  Text(
                    formatDateTime(
                      attempt.endTime.isEmpty
                          ? attempt.startTime
                          : attempt.endTime,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(_formatBand(attempt.overallBandScore)),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: () => _openResult(attempt),
                    icon: const Icon(Icons.bar_chart),
                    label: const Text('Xem kết quả'),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  String _formatBand(double? value) {
    if (value == null) {
      return 'Đang chờ kết quả';
    }
    return 'Overall Band ${value.toStringAsFixed(1)}';
  }
}
