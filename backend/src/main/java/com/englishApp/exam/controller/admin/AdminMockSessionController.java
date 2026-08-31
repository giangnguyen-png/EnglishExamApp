package com.englishApp.exam.controller.admin;

import java.util.Comparator;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.service.MockSessionService;

@Controller
public class AdminMockSessionController {
	private final MockSessionService mockSessionService;

	public AdminMockSessionController(MockSessionService mockSessionService) {
		this.mockSessionService = mockSessionService;
	}

	@GetMapping("/admin/mock-sessions")
	public String list(Model model) {
		model.addAttribute("sessions", this.mockSessionService.findAll().stream()
				.sorted(Comparator.comparing(MockSession::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
				.toList());
		return "admin/mock-sessions/list";
	}

	@PostMapping("/admin/mock-sessions/{id}/delete")
	public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
		try {
			this.mockSessionService.deleteSessionByAdmin(id);
			redirectAttributes.addFlashAttribute("successMessage", "Xóa ca thi thử thành công.");
		} catch (RuntimeException error) {
			redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
		}
		return "redirect:/admin/mock-sessions";
	}
}
