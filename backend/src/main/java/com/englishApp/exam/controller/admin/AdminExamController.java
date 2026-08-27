package com.englishApp.exam.controller.admin;

import java.util.Comparator;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.englishApp.exam.dto.admin.AdminExamForm;
import com.englishApp.exam.model.Exam;
import com.englishApp.exam.repository.ExamSectionRepository;
import com.englishApp.exam.service.ExamService;

@Controller
public class AdminExamController {
	private final ExamService examService;
	private final ExamSectionRepository examSectionRepository;

	public AdminExamController(ExamService examService, ExamSectionRepository examSectionRepository) {
		this.examService = examService;
		this.examSectionRepository = examSectionRepository;
	}

	@GetMapping("/admin/exams")
	public String list(Model model) {
		model.addAttribute("exams", this.examService.findAll());
		return "admin/exams/list";
	}

	@GetMapping("/admin/exams/new")
	public String newExam(Model model) {
		model.addAttribute("examForm", new AdminExamForm());
		model.addAttribute("formTitle", "Tạo đề thi");
		model.addAttribute("actionUrl", "/admin/exams");
		return "admin/exams/form";
	}

	@PostMapping("/admin/exams")
	public String create(@ModelAttribute AdminExamForm form, RedirectAttributes redirectAttributes) {
		try {
			Exam exam = new Exam();
			copyToExam(form, exam);
			Exam savedExam = this.examService.createExam(exam);
			redirectAttributes.addFlashAttribute("successMessage", "Tạo đề thi thành công.");
			return "redirect:/admin/exams/" + savedExam.getId();
		} catch (RuntimeException error) {
			redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
			return "redirect:/admin/exams/new";
		}
	}

	@GetMapping("/admin/exams/{id}")
	public String detail(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
		try {
			Exam exam = this.examService.findById(id);
			model.addAttribute("exam", exam);
			model.addAttribute("sections", this.examSectionRepository.findByExamIdOrderBySectionOrder(id));
			return "admin/exams/detail";
		} catch (RuntimeException error) {
			redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
			return "redirect:/admin/exams";
		}
	}

	@GetMapping("/admin/exams/{id}/edit")
	public String edit(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
		try {
			Exam exam = this.examService.findById(id);
			model.addAttribute("examForm", toForm(exam));
			model.addAttribute("formTitle", "Chỉnh sửa đề thi");
			model.addAttribute("actionUrl", "/admin/exams/" + id);
			return "admin/exams/form";
		} catch (RuntimeException error) {
			redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
			return "redirect:/admin/exams";
		}
	}

	@PostMapping("/admin/exams/{id}")
	public String update(@PathVariable Integer id, @ModelAttribute AdminExamForm form,
			RedirectAttributes redirectAttributes) {
		try {
			Exam updatedExam = new Exam();
			copyToExam(form, updatedExam);
			this.examService.updateExam(id, updatedExam);
			redirectAttributes.addFlashAttribute("successMessage", "Cập nhật đề thi thành công.");
			return "redirect:/admin/exams/" + id;
		} catch (RuntimeException error) {
			redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
			return "redirect:/admin/exams/" + id + "/edit";
		}
	}

	@PostMapping("/admin/exams/{id}/delete")
	public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
		try {
			this.examService.deleteExam(id);
			redirectAttributes.addFlashAttribute("successMessage", "Xóa đề thi thành công.");
		} catch (DataIntegrityViolationException error) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"Không thể xóa đề thi vì đã có bài làm hoặc ca thi thử sử dụng.");
		} catch (RuntimeException error) {
			redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
		}
		return "redirect:/admin/exams";
	}

	private AdminExamForm toForm(Exam exam) {
		AdminExamForm form = new AdminExamForm();
		form.setTitle(exam.getTitle());
		form.setDescription(exam.getDescription());
		form.setPremiumOnly(exam.isPremiumOnly());
		return form;
	}

	private void copyToExam(AdminExamForm form, Exam exam) {
		exam.setTitle(form.getTitle());
		exam.setDescription(form.getDescription());
		exam.setPremiumOnly(form.isPremiumOnly());
	}
}
