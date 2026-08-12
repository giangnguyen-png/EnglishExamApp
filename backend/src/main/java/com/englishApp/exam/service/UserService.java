package com.englishApp.exam.service;

import com.englishApp.exam.model.User;

public interface UserService {
	User register(User user);

	User findById(Integer id);

	User getProfile(Integer userId);

	User updateProfile(Integer userId, User updatedUser);

	void changePassword(Integer userId, String oldPassword, String newPassword);
}
