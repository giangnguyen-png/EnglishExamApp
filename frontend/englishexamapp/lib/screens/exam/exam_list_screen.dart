import 'package:flutter/material.dart';

import '../../models/exam.dart';
import '../../services/api_service.dart';
import '../../services/exam_service.dart';
import 'exam_detail_screen.dart';

class ExamListScreen extends StatefulWidget {
  const ExamListScreen({super.key});

  @override
  State<ExamListScreen> createState() => _ExamListScreenState();
}

class _ExamListScreenState extends State<ExamListScreen> {
  final _examService = ExamService();

  bool _isLoading = true;
  String? _errorMessage;
  List<Exam> _exams = [];

  @override
  void initState() {
    super.initState();
    _loadExams();
  }

  Future<void> _loadExams() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final exams = await _examService.getExams();
      if (!mounted) return;
      setState(() {
        _exams = exams;
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

  void _openExamDetail(Exam exam) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => ExamDetailScreen(examId: exam.id)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Practice Exams')),
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
                onPressed: _loadExams,
                child: const Text('Thu lai'),
              ),
            ],
          ),
        ),
      );
    }

    if (_exams.isEmpty) {
      return const Center(child: Text('Chua co de thi nao.'));
    }

    return RefreshIndicator(
      onRefresh: _loadExams,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _exams.length,
        separatorBuilder: (context, index) => const SizedBox(height: 12),
        itemBuilder: (context, index) {
          final exam = _exams[index];
          return Card(
            child: ListTile(
              title: Text(exam.title),
              subtitle: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (exam.description.isNotEmpty) Text(exam.description),
                  if (exam.premiumOnly)
                    Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: Text(
                        'Premium only',
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.primary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                ],
              ),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => _openExamDetail(exam),
            ),
          );
        },
      ),
    );
  }
}
