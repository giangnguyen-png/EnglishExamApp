import 'package:flutter/material.dart';

import '../models/user.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/payment_service.dart';
import 'auth/login_screen.dart';
import 'expert/expert_home_screen.dart';
import 'exam/exam_list_screen.dart';
import 'history/attempt_history_screen.dart';
import 'premium/my_registrations_screen.dart';
import 'premium/premium_intro_screen.dart';
import 'premium/session_list_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _authService = AuthService();
  final _paymentService = PaymentService();

  User? _user;
  bool _isLoading = true;
  bool _isCheckingPremium = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadCurrentUser();
  }

  Future<void> _loadCurrentUser() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final user = await _authService.getCurrentUser();
      if (!mounted) return;
      if (user.role == 'EXPERT') {
        Navigator.pushReplacement(
          context,
          MaterialPageRoute(builder: (_) => ExpertHomeScreen(user: user)),
        );
        return;
      }
      setState(() {
        _user = user;
      });
    } catch (error) {
      await _authService.deleteToken();
      if (!mounted) return;
      setState(() {
        _errorMessage = ApiService.getErrorMessage(error);
      });
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const LoginScreen()),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _logout() async {
    await _authService.logout();
    if (!mounted) return;
    Navigator.pushAndRemoveUntil(
      context,
      MaterialPageRoute(builder: (_) => const LoginScreen()),
      (_) => false,
    );
  }

  void _openExamList() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const ExamListScreen()),
    );
  }

  void _openAttemptHistory() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const AttemptHistoryScreen()),
    );
  }

  Future<void> _openPremiumSessions() async {
    setState(() {
      _isCheckingPremium = true;
    });

    try {
      final premium = await _paymentService.getPremiumStatus();
      if (!mounted) return;
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) =>
              premium ? const SessionListScreen() : const PremiumIntroScreen(),
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
          _isCheckingPremium = false;
        });
      }
    }
  }

  void _openMyRegistrations() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const MyRegistrationsScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final user = _user;

    return Scaffold(
      appBar: AppBar(
        title: Text(user == null ? 'Home' : 'Xin chao, ${user.displayName}'),
        actions: [
          IconButton(
            onPressed: _logout,
            icon: const Icon(Icons.logout),
            tooltip: 'Dang xuat',
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (_errorMessage != null)
                    Text(
                      _errorMessage!,
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.error,
                      ),
                    ),
                  if (user != null) ...[
                    Text(
                      'Xin chao, ${user.displayName}',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: 8),
                    Text('Role: ${user.role}'),
                    const SizedBox(height: 24),
                  ],
                  FilledButton.icon(
                    onPressed: _openExamList,
                    icon: const Icon(Icons.menu_book),
                    label: const Text('Luyện thi'),
                  ),
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: _isCheckingPremium ? null : _openPremiumSessions,
                    icon: _isCheckingPremium
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.workspace_premium),
                    label: const Text('Thi thử Premium'),
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: _openMyRegistrations,
                    icon: const Icon(Icons.badge),
                    label: const Text('Ca thi đã đăng ký'),
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: _openAttemptHistory,
                    icon: const Icon(Icons.history),
                    label: const Text('Lịch sử làm bài'),
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: _logout,
                    icon: const Icon(Icons.logout),
                    label: const Text('Đăng xuất'),
                  ),
                ],
              ),
      ),
    );
  }
}
