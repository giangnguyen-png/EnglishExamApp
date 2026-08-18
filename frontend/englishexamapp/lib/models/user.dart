class User {
  final int id;
  final String username;
  final String email;
  final String fullName;
  final String role;

  User({
    required this.id,
    required this.username,
    required this.email,
    required this.fullName,
    required this.role,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] as int,
      username: json['username'] as String,
      email: json['email'] as String,
      fullName: (json['fullName'] as String?) ?? '',
      role: json['role'] as String,
    );
  }

  String get displayName => fullName.isNotEmpty ? fullName : username;
}
