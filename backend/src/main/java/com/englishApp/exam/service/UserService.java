package com.englishApp.exam.service;

import com.englishApp.exam.model.User;

public interface UserService {
	User register(User user);

	User findById(Integer id);

	User findByUsername(String username);

	User getProfile(Integer userId);

	User updateProfile(Integer userId, User updatedUser);

	User updateUserByAdmin(Integer targetUserId, User updatedUser, Integer roleId, Integer currentAdminId);

	void deleteUserByAdmin(Integer targetUserId, Integer currentAdminId);

	void changePassword(Integer userId, String oldPassword, String newPassword);
}
