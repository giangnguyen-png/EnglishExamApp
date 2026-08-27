package com.englishApp.exam.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.englishApp.exam.model.Role;
import com.englishApp.exam.model.User;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.RoleRepository;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.service.UserService;

@Service
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final MockSessionRepository mockSessionRepository;

	public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
			MockSessionRepository mockSessionRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.mockSessionRepository = mockSessionRepository;
	}

	public User register(User user) {
		if (this.userRepository.existsByUsername(user.getUsername())) {
			throw new RuntimeException("Username already exists");
		}
	
		if (this.userRepository.existsByEmail(user.getEmail())) {
			throw new RuntimeException("Email already exists");
		}
	
		user.setRole(this.roleRepository.findByName("USER").orElseThrow(() -> new RuntimeException("Default role not found")));
		user.setPassword(this.passwordEncoder.encode(user.getPassword()));
		return this.userRepository.save(user);
	}

	public User findById(Integer id) {
		return this.userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
	}

	public User findByUsername(String username) {
		return this.userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
	}

	public User getProfile(Integer userId) {
		return this.findById(userId);
	}

	public User updateProfile(Integer userId, User updatedUser) {
		User existingUser = this.findById(userId);
	
		if (!existingUser.getEmail().equals(updatedUser.getEmail())
				&& this.userRepository.existsByEmail(updatedUser.getEmail())) {
			throw new RuntimeException("Email already exists");
		}
	
		existingUser.setFullName(updatedUser.getFullName());
		existingUser.setEmail(updatedUser.getEmail());
	
		return this.userRepository.save(existingUser);
	}

	@Transactional
	public User updateUserByAdmin(Integer targetUserId, User updatedUser, Integer roleId, Integer currentAdminId) {
		User existingUser = this.findById(targetUserId);
		validateAdminUserUpdate(existingUser, updatedUser);

		Role selectedRole = this.roleRepository.findById(roleId)
				.orElseThrow(() -> new RuntimeException("Vai trò không tồn tại."));
		boolean selfUpdate = targetUserId.equals(currentAdminId);
		if (selfUpdate && !existingUser.getRole().getId().equals(selectedRole.getId())) {
			throw new RuntimeException("Bạn không thể thay đổi vai trò của chính mình.");
		}

		existingUser.setUsername(updatedUser.getUsername().trim());
		existingUser.setEmail(updatedUser.getEmail().trim());
		if (!selfUpdate) {
			existingUser.setRole(selectedRole);
		}
		return this.userRepository.save(existingUser);
	}

	@Transactional
	public void deleteUserByAdmin(Integer targetUserId, Integer currentAdminId) {
		if (targetUserId.equals(currentAdminId)) {
			throw new RuntimeException("Bạn không thể xóa tài khoản đang đăng nhập.");
		}
		User user = this.findById(targetUserId);
		String roleName = user.getRole() == null ? "" : user.getRole().getName();
		if ("ADMIN".equals(roleName)) {
			throw new RuntimeException("Không thể xóa tài khoản quản trị viên.");
		}
		if ("EXPERT".equals(roleName) && this.mockSessionRepository.existsByExpertId(targetUserId)) {
			throw new RuntimeException("Không thể xóa giám khảo đã có ca thi thử.");
		}
		this.userRepository.delete(user);
	}

	public void changePassword(Integer userId, String oldPassword, String newPassword) {
		User existingUser = this.findById(userId);
	
		if (!this.passwordEncoder.matches(oldPassword, existingUser.getPassword())) {
			throw new RuntimeException("Old password is incorrect");
		}
	
		existingUser.setPassword(this.passwordEncoder.encode(newPassword));
		this.userRepository.save(existingUser);
	}

	private void validateAdminUserUpdate(User existingUser, User updatedUser) {
		if (updatedUser.getUsername() == null || updatedUser.getUsername().isBlank()) {
			throw new RuntimeException("Tên đăng nhập không được để trống.");
		}
		if (updatedUser.getEmail() == null || updatedUser.getEmail().isBlank()) {
			throw new RuntimeException("Email không được để trống.");
		}
		if (!updatedUser.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
			throw new RuntimeException("Email không hợp lệ.");
		}
		if (this.userRepository.existsByUsernameAndIdNot(updatedUser.getUsername().trim(), existingUser.getId())) {
			throw new RuntimeException("Tên đăng nhập đã tồn tại.");
		}
		if (this.userRepository.existsByEmailAndIdNot(updatedUser.getEmail().trim(), existingUser.getId())) {
			throw new RuntimeException("Email đã tồn tại.");
		}
	}
}
