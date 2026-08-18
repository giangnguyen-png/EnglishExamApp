import 'package:flutter/material.dart';

import '../../models/exam.dart';
import '../../models/mock_session.dart';
import '../../services/api_service.dart';
import '../../services/exam_service.dart';
import '../../services/expert_service.dart';

class SessionFormScreen extends StatefulWidget {
  final MockSession? session;

  const SessionFormScreen({super.key, this.session});

  @override
  State<SessionFormScreen> createState() => _SessionFormScreenState();
}

class _SessionFormScreenState extends State<SessionFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _expertService = ExpertService();
  final _examService = ExamService();
  final _roomController = TextEditingController();
  final _maxCandidatesController = TextEditingController(text: '20');

  List<Exam> _exams = [];
  int? _examId;
  DateTime? _startTime;
  DateTime? _endTime;
  DateTime? _registrationDeadline;
  bool _isLoading = true;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    final session = widget.session;
    if (session != null) {
      _examId = session.examId;
      _roomController.text = session.roomCode;
      _maxCandidatesController.text = session.maxCandidates.toString();
      _startTime = DateTime.tryParse(session.startTime);
      _endTime = DateTime.tryParse(session.endTime);
      _registrationDeadline = DateTime.tryParse(session.registrationDeadline);
    }
    _loadExams();
  }

  @override
  void dispose() {
    _roomController.dispose();
    _maxCandidatesController.dispose();
    super.dispose();
  }

  Future<void> _loadExams() async {
    try {
      final exams = await _examService.getExams();
      if (!mounted) return;
      setState(() {
        _exams = exams;
        _examId ??= exams.isEmpty ? null : exams.first.id;
      });
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _pickDateTime(ValueChanged<DateTime> onPicked) async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      firstDate: DateTime(now.year - 1),
      lastDate: DateTime(now.year + 3),
      initialDate: now,
    );
    if (date == null || !mounted) return;
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(now),
    );
    if (time == null) return;
    onPicked(DateTime(date.year, date.month, date.day, time.hour, time.minute));
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    if (_examId == null || _startTime == null || _endTime == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng chọn Exam, Start và End time.')),
      );
      return;
    }

    setState(() {
      _isSaving = true;
    });

    try {
      final maxCandidates = int.parse(_maxCandidatesController.text.trim());
      final session = widget.session;
      if (session == null) {
        await _expertService.createSession(
          examId: _examId!,
          roomCode: _roomController.text.trim(),
          startTime: _toBackendDateTime(_startTime!),
          endTime: _toBackendDateTime(_endTime!),
          registrationDeadline: _registrationDeadline == null
              ? ''
              : _toBackendDateTime(_registrationDeadline!),
          maxCandidates: maxCandidates,
        );
      } else {
        await _expertService.updateSession(
          sessionId: session.id,
          examId: _examId!,
          roomCode: _roomController.text.trim(),
          startTime: _toBackendDateTime(_startTime!),
          endTime: _toBackendDateTime(_endTime!),
          registrationDeadline: _registrationDeadline == null
              ? ''
              : _toBackendDateTime(_registrationDeadline!),
          maxCandidates: maxCandidates,
        );
      }
      if (!mounted) return;
      Navigator.pop(context);
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ApiService.getErrorMessage(error))),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSaving = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.session == null ? 'Tạo ca thi' : 'Chỉnh sửa ca thi'),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  DropdownButtonFormField<int>(
                    initialValue: _examId,
                    items: _exams
                        .map(
                          (exam) => DropdownMenuItem(
                            value: exam.id,
                            child: Text(exam.title),
                          ),
                        )
                        .toList(),
                    onChanged: (value) => setState(() => _examId = value),
                    decoration: const InputDecoration(
                      labelText: 'Exam',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _roomController,
                    decoration: const InputDecoration(
                      labelText: 'Room code',
                      border: OutlineInputBorder(),
                    ),
                    validator: (value) => value == null || value.trim().isEmpty
                        ? 'Nhập room code'
                        : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _maxCandidatesController,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(
                      labelText: 'Max candidates',
                      border: OutlineInputBorder(),
                    ),
                    validator: (value) {
                      final number = int.tryParse(value ?? '');
                      if (number == null || number <= 0) {
                        return 'Nhập số thí sinh hợp lệ';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 12),
                  _DateButton(
                    label: 'Start time',
                    value: _startTime,
                    onPressed: () => _pickDateTime(
                      (value) => setState(() => _startTime = value),
                    ),
                  ),
                  _DateButton(
                    label: 'End time',
                    value: _endTime,
                    onPressed: () => _pickDateTime(
                      (value) => setState(() => _endTime = value),
                    ),
                  ),
                  _DateButton(
                    label: 'Registration deadline',
                    value: _registrationDeadline,
                    onPressed: () => _pickDateTime(
                      (value) => setState(() => _registrationDeadline = value),
                    ),
                  ),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: _isSaving ? null : _save,
                    child: _isSaving
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('Lưu'),
                  ),
                ],
              ),
            ),
    );
  }
}

class _DateButton extends StatelessWidget {
  final String label;
  final DateTime? value;
  final VoidCallback onPressed;

  const _DateButton({
    required this.label,
    required this.value,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: OutlinedButton(
        onPressed: onPressed,
        child: Align(
          alignment: Alignment.centerLeft,
          child: Text(
            '$label: ${value == null ? 'Chọn' : _formatDateTime(value!)}',
          ),
        ),
      ),
    );
  }
}

String _toBackendDateTime(DateTime date) {
  return '${date.year.toString().padLeft(4, '0')}-'
      '${date.month.toString().padLeft(2, '0')}-'
      '${date.day.toString().padLeft(2, '0')}T'
      '${date.hour.toString().padLeft(2, '0')}:'
      '${date.minute.toString().padLeft(2, '0')}:00';
}

String _formatDateTime(DateTime date) {
  return '${date.day.toString().padLeft(2, '0')}/'
      '${date.month.toString().padLeft(2, '0')}/${date.year} '
      '${date.hour.toString().padLeft(2, '0')}:'
      '${date.minute.toString().padLeft(2, '0')}';
}
