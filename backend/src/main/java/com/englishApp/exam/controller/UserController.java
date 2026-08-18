package com.englishApp.exam.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.englishApp.exam.dto.user.ChangePasswordRequest;
import com.englishApp.exam.dto.user.UpdateProfileRequest;
import com.englishApp.exam.dto.user.UserProfileResponse;
import com.englishApp.exam.model.User;
import com.englishApp.exam.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		return ResponseEntity.ok(UserProfileResponse.from(currentUser));
	}

	@PutMapping("/me")
	public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
			Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		User updatedUser = new User();
		updatedUser.setEmail(request.email());
		updatedUser.setFullName(request.fullName());
		return ResponseEntity.ok(UserProfileResponse.from(this.userService.updateProfile(currentUser.getId(), updatedUser)));
	}

	@PutMapping("/me/password")
	public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
			Authentication authentication) {
		User currentUser = getCurrentUser(authentication);
		this.userService.changePassword(currentUser.getId(), request.oldPassword(), request.newPassword());
		return ResponseEntity.noContent().build();
	}

	private User getCurrentUser(Authentication authentication) {
		return this.userService.findByUsername(authentication.getName());
	}
}
