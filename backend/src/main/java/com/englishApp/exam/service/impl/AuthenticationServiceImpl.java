package com.englishApp.exam.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.englishApp.exam.dto.auth.LoginRequest;
import com.englishApp.exam.dto.auth.LoginResponse;
import com.englishApp.exam.service.AuthenticationService;
import com.englishApp.exam.service.JwtService;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthenticationServiceImpl(AuthenticationManager authenticationManager, JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	public LoginResponse login(LoginRequest request) {
		Authentication authentication = this.authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password()));

		String accessToken = this.jwtService.generateAccessToken(authentication);
		return new LoginResponse(accessToken, "Bearer");
	}
}
