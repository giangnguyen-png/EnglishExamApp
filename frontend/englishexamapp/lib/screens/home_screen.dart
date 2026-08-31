import 'package:flutter/material.dart';

import '../config/app_colors.dart';
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
        title: Text(user == null ? 'Home' : 'Xin chào, ${user.displayName}'),
        actions: [
          IconButton(
            onPressed: _logout,
            icon: const Icon(Icons.logout),
            tooltip: 'Đăng xuất',
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
                      'Xin chào, ${user.displayName}',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: 8),
                    Text('Bạn muốn luyện gì hôm nay?'),
                    const SizedBox(height: 24),
                  ],
                  _HomeActionCard(
                    icon: Icons.menu_book,
                    title: 'Luyện thi',
                    subtitle: 'Làm các bài IELTS đủ 4 kỹ năng',
                    accentColor: AppColors.primary,
                    onTap: _openExamList,
                  ),
            
                  const SizedBox(height: 12),
                  _HomeActionCard(
                    icon: _isCheckingPremium
                        ? Icons.hourglass_top
                        : Icons.workspace_premium,
                    title: 'Thi thử Premium',
                    subtitle: 'Thi theo ca với Expert và nhận số báo danh',
                    accentColor: AppColors.premium,
                    onTap: _isCheckingPremium ? null : _openPremiumSessions,
                  ),
                  const SizedBox(height: 12),
                  _HomeActionCard(
                    icon: Icons.badge,
                    title: 'Ca thi đã đăng ký',
                    subtitle: 'Xem số báo danh và vào thi Premium',
                    accentColor: AppColors.premium,
                    onTap: _openMyRegistrations,
                  ),
                  const SizedBox(height: 12),
                  _HomeActionCard(
                    icon: Icons.history,
                    title: 'Lịch sử làm bài',
                    subtitle: 'Xem lại điểm và nhận xét AI',
                    accentColor: AppColors.reading,
                    onTap: _openAttemptHistory,
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

class _HomeActionCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final Color accentColor;
  final VoidCallback? onTap;

  const _HomeActionCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.accentColor,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        leading: CircleAvatar(
          backgroundColor: AppColors.soft(accentColor),
          child: Icon(icon, color: accentColor),
        ),
        title: Text(title),
        subtitle: Text(subtitle),
        trailing: const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }
}

class _SkillIdentity extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;

  const _SkillIdentity({
    required this.icon,
    required this.label,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Chip(
      avatar: Icon(icon, color: color, size: 18),
      label: Text(label),
      backgroundColor: AppColors.soft(color),
      side: BorderSide(color: color.withOpacity(0.32)),
    );
  }
}
