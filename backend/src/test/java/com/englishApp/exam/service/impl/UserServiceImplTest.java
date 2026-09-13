package com.englishApp.exam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.englishApp.exam.model.Role;
import com.englishApp.exam.model.User;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.RoleRepository;
import com.englishApp.exam.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
	@Mock
	private UserRepository userRepository;
	@Mock
	private RoleRepository roleRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private MockSessionRepository mockSessionRepository;

	private UserServiceImpl service;

	@BeforeEach
	void setUp() {
		this.service = new UserServiceImpl(this.userRepository, this.roleRepository, this.passwordEncoder,
				this.mockSessionRepository);
	}

	@Test
	void register_whenUsernameAndEmailAreAvailable_shouldAssignUserRoleAndEncodePassword() {
		User user = user(1, "learner", "learner@example.com", role(1, "USER"));
		user.setPassword("secret123");
		Role defaultRole = role(2, "USER");
		when(this.userRepository.existsByUsername("learner")).thenReturn(false);
		when(this.userRepository.existsByEmail("learner@example.com")).thenReturn(false);
		when(this.roleRepository.findByName("USER")).thenReturn(Optional.of(defaultRole));
		when(this.passwordEncoder.encode("secret123")).thenReturn("encoded");
		when(this.userRepository.save(user)).thenReturn(user);

		User registered = this.service.register(user);

		assertSame(user, registered);
		assertSame(defaultRole, registered.getRole());
		assertEquals("encoded", registered.getPassword());
	}

	@Test
	void register_whenUsernameExists_shouldRejectRegistration() {
		User user = user(1, "learner", "learner@example.com", null);
		when(this.userRepository.existsByUsername("learner")).thenReturn(true);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> this.service.register(user));

		assertEquals("Username already exists", exception.getMessage());
		verify(this.userRepository, never()).save(any(User.class));
	}

	@Test
	void updateProfile_whenEmailBelongsToAnotherUser_shouldRejectUpdate() {
		User existing = user(1, "learner", "old@example.com", role(1, "USER"));
		User updated = user(1, "learner", "new@example.com", role(1, "USER"));
		when(this.userRepository.findById(1)).thenReturn(Optional.of(existing));
		when(this.userRepository.existsByEmail("new@example.com")).thenReturn(true);

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.updateProfile(1, updated));

		assertEquals("Email already exists", exception.getMessage());
	}

	@Test
	void updateUserByAdmin_whenAdminUpdatesAnotherUser_shouldTrimFieldsAndChangeRole() {
		User existing = user(2, "old", "old@example.com", role(1, "USER"));
		User updated = user(2, "  expert  ", "expert@example.com", null);
		Role expertRole = role(3, "EXPERT");
		when(this.userRepository.findById(2)).thenReturn(Optional.of(existing));
		when(this.userRepository.existsByUsernameAndIdNot("expert", 2)).thenReturn(false);
		when(this.userRepository.existsByEmailAndIdNot("expert@example.com", 2)).thenReturn(false);
		when(this.roleRepository.findById(3)).thenReturn(Optional.of(expertRole));
		when(this.userRepository.save(existing)).thenReturn(existing);

		User saved = this.service.updateUserByAdmin(2, updated, 3, 1);

		assertSame(existing, saved);
		assertEquals("expert", existing.getUsername());
		assertEquals("expert@example.com", existing.getEmail());
		assertSame(expertRole, existing.getRole());
	}

	@Test
	void updateUserByAdmin_whenAdminChangesOwnRole_shouldRejectUpdate() {
		User existing = user(1, "admin", "admin@example.com", role(1, "ADMIN"));
		User updated = user(1, "admin", "admin@example.com", null);
		when(this.userRepository.findById(1)).thenReturn(Optional.of(existing));
		when(this.roleRepository.findById(2)).thenReturn(Optional.of(role(2, "USER")));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.updateUserByAdmin(1, updated, 2, 1));

		assertEquals("Bạn không thể thay đổi vai trò của chính mình.", exception.getMessage());
	}

	@Test
	void deleteUserByAdmin_whenDeletingSelf_shouldRejectDelete() {
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.deleteUserByAdmin(1, 1));

		assertEquals("Bạn không thể xóa tài khoản đang đăng nhập.", exception.getMessage());
		verify(this.userRepository, never()).delete(any(User.class));
	}

	@Test
	void deleteUserByAdmin_whenExpertHasMockSessions_shouldRejectDelete() {
		User expert = user(2, "expert", "expert@example.com", role(3, "EXPERT"));
		when(this.userRepository.findById(2)).thenReturn(Optional.of(expert));
		when(this.mockSessionRepository.existsByExpertId(2)).thenReturn(true);

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.deleteUserByAdmin(2, 1));

		assertEquals("Không thể xóa giám khảo đã có ca thi thử.", exception.getMessage());
		verify(this.userRepository, never()).delete(any(User.class));
	}

	@Test
	void changePassword_whenOldPasswordMatches_shouldEncodeAndSaveNewPassword() {
		User existing = user(1, "learner", "learner@example.com", role(1, "USER"));
		existing.setPassword("old-hash");
		when(this.userRepository.findById(1)).thenReturn(Optional.of(existing));
		when(this.passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
		when(this.passwordEncoder.encode("new-password")).thenReturn("new-hash");

		this.service.changePassword(1, "old-password", "new-password");

		assertEquals("new-hash", existing.getPassword());
		verify(this.userRepository).save(existing);
	}

	@Test
	void changePassword_whenOldPasswordDoesNotMatch_shouldRejectChange() {
		User existing = user(1, "learner", "learner@example.com", role(1, "USER"));
		existing.setPassword("old-hash");
		when(this.userRepository.findById(1)).thenReturn(Optional.of(existing));
		when(this.passwordEncoder.matches("bad-password", "old-hash")).thenReturn(false);

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> this.service.changePassword(1, "bad-password", "new-password"));

		assertEquals("Old password is incorrect", exception.getMessage());
		verify(this.userRepository, never()).save(any(User.class));
	}

	private User user(Integer id, String username, String email, Role role) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		user.setEmail(email);
		user.setFullName(username);
		user.setRole(role);
		return user;
	}

	private Role role(Integer id, String name) {
		Role role = new Role();
		role.setId(id);
		role.setName(name);
		return role;
	}
}
