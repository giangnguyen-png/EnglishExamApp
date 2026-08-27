package com.englishApp.exam.controller.admin;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.service.MockSessionService;

@Controller
public class AdminMockSessionController {
	private final MockSessionRepository mockSessionRepository;
	private final MockSessionService mockSessionService;

	public AdminMockSessionController(MockSessionRepository mockSessionRepository, MockSessionService mockSessionService) {
		this.mockSessionRepository = mockSessionRepository;
		this.mockSessionService = mockSessionService;
	}

	@GetMapping("/admin/mock-sessions")
	public String list(Model model) {
		model.addAttribute("sessions", this.mockSessionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
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
