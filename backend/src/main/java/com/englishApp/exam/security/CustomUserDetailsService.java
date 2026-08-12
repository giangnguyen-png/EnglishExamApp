package com.englishApp.exam.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.englishApp.exam.model.Role;
import com.englishApp.exam.model.User;
import com.englishApp.exam.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = this.userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		return org.springframework.security.core.userdetails.User
				.withUsername(user.getUsername())
				.password(user.getPassword())
				.authorities(toAuthority(user.getRole()))
				.build();
	}

	private List<SimpleGrantedAuthority> toAuthority(Role role) {
		String roleName = role.getName();
		String authority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
		return List.of(new SimpleGrantedAuthority(authority));
	}
}
