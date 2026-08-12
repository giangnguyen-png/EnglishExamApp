package com.englishApp.exam.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.englishApp.exam.service.JwtService;

@Service
public class JwtServiceImpl implements JwtService {
	private final JwtEncoder jwtEncoder;
	private final long accessTokenExpirationMinutes;

	public JwtServiceImpl(JwtEncoder jwtEncoder,
			@Value("${jwt.access-token-expiration-minutes}") long accessTokenExpirationMinutes) {
		this.jwtEncoder = jwtEncoder;
		this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
	}

	public String generateAccessToken(Authentication authentication) {
		Instant now = Instant.now();
		List<String> roles = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith("ROLE_"))
				.map(authority -> authority.substring("ROLE_".length()))
				.toList();

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(authentication.getName())
				.issuedAt(now)
				.expiresAt(now.plusSeconds(this.accessTokenExpirationMinutes * 60))
				.claim("roles", roles)
				.build();

		return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}
}
