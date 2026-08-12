package com.englishApp.exam.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.englishApp.exam.model.User;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.service.UserService;

@Service
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public User register(User user) {
		if (this.userRepository.existsByUsername(user.getUsername())) {
			throw new RuntimeException("Username already exists");
		}
	
		if (this.userRepository.existsByEmail(user.getEmail())) {
			throw new RuntimeException("Email already exists");
		}
	
		user.setPassword(this.passwordEncoder.encode(user.getPassword()));
		return this.userRepository.save(user);
	}

	public User findById(Integer id) {
		return this.userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
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

	public void changePassword(Integer userId, String oldPassword, String newPassword) {
		User existingUser = this.findById(userId);
	
		if (!this.passwordEncoder.matches(oldPassword, existingUser.getPassword())) {
			throw new RuntimeException("Old password is incorrect");
		}
	
		existingUser.setPassword(this.passwordEncoder.encode(newPassword));
		this.userRepository.save(existingUser);
	}
}
