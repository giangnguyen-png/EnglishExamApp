package com.englishApp.exam.service;

import com.englishApp.exam.dto.auth.LoginRequest;
import com.englishApp.exam.dto.auth.LoginResponse;

public interface AuthenticationService {
	LoginResponse login(LoginRequest request);
}
