package com.englishApp.exam.controller.admin;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.englishApp.exam.dto.admin.AdminUserForm;
import com.englishApp.exam.model.User;
import com.englishApp.exam.repository.RoleRepository;
import com.englishApp.exam.repository.SessionRegistrationRepository;
import com.englishApp.exam.repository.TestAttemptRepository;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.service.UserService;

@Controller
public class AdminUserController {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final TestAttemptRepository testAttemptRepository;
	private final SessionRegistrationRepository sessionRegistrationRepository;
	private final UserService userService;

	public AdminUserController(UserRepository userRepository, RoleRepository roleRepository,
			TestAttemptRepository testAttemptRepository, SessionRegistrationRepository sessionRegistrationRepository,
			UserService userService) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.testAttemptRepository = testAttemptRepository;
		this.sessionRegistrationRepository = sessionRegistrationRepository;
		this.userService = userService;
	}

	@GetMapping("/admin/users")
	public String list(@RequestParam(name = "q", required = false) String query, Model model) {
		List<User> users = this.userRepository.findAll().stream()
				.filter(user -> matchesSearch(user, query))
				.sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
		model.addAttribute("users", users);
		model.addAttribute("query", query == null ? "" : query);
		return "admin/users/list";
	}

	@GetMapping("/admin/users/{id}")
	public String detail(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
		return this.userRepository.findById(id).map(user -> {
			model.addAttribute("user", user);
			model.addAttribute("attemptCount", this.testAttemptRepository.findByUserId(id).size());
			model.addAttribute("registrationCount", this.sessionRegistrationRepository.findByUserId(id).size());
			return "admin/users/detail";
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
			return "redirect:/admin/users";
		});
	}

	@GetMapping("/admin/users/{id}/edit")
	public String edit(@PathVariable Integer id, Model model, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		return this.userRepository.findById(id).map(user -> {
			model.addAttribute("user", user);
			model.addAttribute("userForm", toForm(user));
			model.addAttribute("roles", this.roleRepository.findAll());
			model.addAttribute("isSelf", user.getUsername().equals(authentication.getName()));
			return "admin/users/form";
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
			return "redirect:/admin/users";
		});
	}

	@PostMapping("/admin/users/{id}/edit")
	public String update(@PathVariable Integer id, @ModelAttribute AdminUserForm form, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			User currentAdmin = this.userService.findByUsername(authentication.getName());
			User updatedUser = new User();
			updatedUser.setUsername(form.getUsername());
			updatedUser.setEmail(form.getEmail());
			User savedUser = this.userService.updateUserByAdmin(id, updatedUser, form.getRoleId(), currentAdmin.getId());
			if (id.equals(currentAdmin.getId())) {
				refreshAuthentication(savedUser, authentication);
			}
			redirectAttributes.addFlashAttribute("successMessage", "Cập nhật người dùng thành công.");
			return "redirect:/admin/users/" + id;
		} catch (RuntimeException error) {
			redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
			return "redirect:/admin/users/" + id + "/edit";
		}
	}

	@PostMapping("/admin/users/{id}/delete")
	public String delete(@PathVariable Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
		try {
			User currentAdmin = this.userService.findByUsername(authentication.getName());
			this.userService.deleteUserByAdmin(id, currentAdmin.getId());
			redirectAttributes.addFlashAttribute("successMessage", "Xóa người dùng thành công.");
		} catch (RuntimeException error) {
			redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
		}
		return "redirect:/admin/users";
	}

	private boolean matchesSearch(User user, String query) {
		if (query == null || query.isBlank()) {
			return true;
		}
		String normalizedQuery = query.toLowerCase();
		return contains(user.getUsername(), normalizedQuery) || contains(user.getEmail(), normalizedQuery)
				|| contains(user.getFullName(), normalizedQuery);
	}

	private boolean contains(String value, String query) {
		return value != null && value.toLowerCase().contains(query);
	}

	private AdminUserForm toForm(User user) {
		AdminUserForm form = new AdminUserForm();
		form.setUsername(user.getUsername());
		form.setEmail(user.getEmail());
		if (user.getRole() != null) {
			form.setRoleId(user.getRole().getId());
		}
		return form;
	}

	private void refreshAuthentication(User user, Authentication authentication) {
		UsernamePasswordAuthenticationToken refreshedAuthentication = new UsernamePasswordAuthenticationToken(
				user.getUsername(), authentication.getCredentials(), authentication.getAuthorities());
		refreshedAuthentication.setDetails(authentication.getDetails());
		SecurityContextHolder.getContext().setAuthentication(refreshedAuthentication);
	}
}
