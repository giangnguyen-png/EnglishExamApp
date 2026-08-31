import 'package:flutter/material.dart';

import '../../config/app_colors.dart';
import '../../models/user.dart';
import '../../services/auth_service.dart';
import '../../widgets/accent_card.dart';
import '../auth/login_screen.dart';
import 'expert_session_list_screen.dart';

class ExpertHomeScreen extends StatelessWidget {
  final User user;

  const ExpertHomeScreen({super.key, required this.user});

  Future<void> _logout(BuildContext context) async {
    await AuthService().logout();
    if (!context.mounted) return;
    Navigator.pushAndRemoveUntil(
      context,
      MaterialPageRoute(builder: (_) => const LoginScreen()),
      (_) => false,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Xin chào, ${user.displayName}')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            AccentCard(
              color: AppColors.expert,
              child: Row(
                children: [
                  const Icon(Icons.verified_user, color: AppColors.expert),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Expert',
                          style: Theme.of(context)
                              .textTheme
                              .headlineSmall
                              ?.copyWith(color: AppColors.expert),
                        ),
                        const SizedBox(height: 6),
                        const Text(
                          'Quản lý kỳ thi và chấm Speaking cho thí sinh.',
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            FilledButton.icon(
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => const ExpertSessionListScreen(),
                ),
              ),
              icon: const Icon(Icons.event),
              label: const Text('Quản lý ca thi'),
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: () => _logout(context),
              icon: const Icon(Icons.logout),
              label: const Text('Đăng xuất'),
            ),
          ],
        ),
      ),
    );
  }
}
