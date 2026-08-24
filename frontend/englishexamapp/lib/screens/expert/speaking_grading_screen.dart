import 'package:flutter/material.dart';
import 'package:just_audio/just_audio.dart';

import '../../models/mock_session.dart';
import '../../services/api_service.dart';
import '../../services/expert_service.dart';

class SpeakingGradingScreen extends StatefulWidget {
  final SpeakingAttempt attempt;

  const SpeakingGradingScreen({super.key, required this.attempt});

  @override
  State<SpeakingGradingScreen> createState() => _SpeakingGradingScreenState();
}

class _SpeakingGradingScreenState extends State<SpeakingGradingScreen> {
  final _expertService = ExpertService();
  final _audioPlayer = AudioPlayer();
  final _scoreController = TextEditingController();

  String? _playingUrl;
  bool _isSaving = false;

  @override
  void dispose() {
    _audioPlayer.dispose();
    _scoreController.dispose();
    super.dispose();
  }

  Future<void> _toggleAudio(String audioUrl) async {
    try {
      if (_playingUrl == audioUrl && _audioPlayer.playing) {
        await _audioPlayer.pause();
        if (!mounted) return;
        setState(() {});
        return;
      }

      if (_playingUrl != audioUrl) {
        await _audioPlayer.stop();
        await _audioPlayer.setUrl(audioUrl);
      }
      _audioPlayer.play();
      if (!mounted) return;
      setState(() {
        _playingUrl = audioUrl;
      });
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Không thể phát audio: $error')));
    }
  }

  Future<void> _grade() async {
    final score = double.tryParse(_scoreController.text.trim());
    if (score == null || score < 0 || score > 9) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Nhập Speaking Band từ 0.0 đến 9.0.')),
      );
      return;
    }

    setState(() {
      _isSaving = true;
    });

    try {
      await _expertService.gradeSpeaking(widget.attempt.attemptId, score);
      if (!mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('Đã chấm Speaking.')));
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
    final attempt = widget.attempt;
    return Scaffold(
      appBar: AppBar(title: Text('SBD ${attempt.candidateNumber}')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            attempt.username,
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 6),
          Text(
            'Speaking Test',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 16),
          ...attempt.responses.map(_buildResponse),
          const SizedBox(height: 16),
          TextField(
            controller: _scoreController,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(labelText: 'Speaking Band'),
          ),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: _isSaving ? null : _grade,
            child: _isSaving
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Chấm điểm'),
          ),
        ],
      ),
    );
  }

  Widget _buildResponse(SpeakingResponse response) {
    final isPlaying = _playingUrl == response.audioUrl && _audioPlayer.playing;
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Question', style: Theme.of(context).textTheme.labelLarge),
            const SizedBox(height: 6),
            Text(
              response.questionContent,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            FilledButton.icon(
              onPressed: response.audioUrl.isEmpty
                  ? null
                  : () => _toggleAudio(response.audioUrl),
              icon: Icon(isPlaying ? Icons.pause : Icons.play_arrow),
              label: Text(isPlaying ? 'Tạm dừng' : 'Phát audio'),
            ),
            const SizedBox(height: 8),
            const Text('Transcript'),
            Text(
              response.transcript.isEmpty
                  ? 'Chưa có transcript'
                  : response.transcript,
            ),
          ],
        ),
      ),
    );
  }
}
