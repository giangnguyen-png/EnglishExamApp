package com.englishApp.exam.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.englishApp.exam.dto.auth.LoginRequest;
import com.englishApp.exam.dto.auth.LoginResponse;
import com.englishApp.exam.dto.auth.RegisterRequest;
import com.englishApp.exam.dto.user.UserProfileResponse;
import com.englishApp.exam.model.User;
import com.englishApp.exam.service.AuthenticationService;
import com.englishApp.exam.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthenticationService authenticationService;
	private final UserService userService;

	public AuthController(AuthenticationService authenticationService, UserService userService) {
		this.authenticationService = authenticationService;
		this.userService = userService;
	}

	@PostMapping("/register")
	public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
		User user = new User();
		user.setUsername(request.username());
		user.setEmail(request.email());
		user.setPassword(request.password());
		user.setFullName(resolveFullName(request));
		User registeredUser = this.userService.register(user);
		return ResponseEntity.ok(UserProfileResponse.from(registeredUser));
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(this.authenticationService.login(request));
	}

	private String resolveFullName(RegisterRequest request) {
		if (request.fullName() == null || request.fullName().isBlank()) {
			return request.username();
		}
		return request.fullName();
	}
}
