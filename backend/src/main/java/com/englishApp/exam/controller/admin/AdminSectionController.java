package com.englishApp.exam.controller.admin;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.englishApp.exam.dto.admin.AdminSectionForm;
import com.englishApp.exam.model.Exam;
import com.englishApp.exam.model.ExamSection;
import com.englishApp.exam.model.enums.SkillType;
import com.englishApp.exam.repository.ExamSectionRepository;
import com.englishApp.exam.service.ExamService;

@Controller
public class AdminSectionController {
	private final ExamService examService;
	private final ExamSectionRepository examSectionRepository;

	public AdminSectionController(ExamService examService, ExamSectionRepository examSectionRepository) {
		this.examService = examService;
		this.examSectionRepository = examSectionRepository;
	}

	@GetMapping("/admin/exams/{examId}/sections/new")
	public String newSection(@PathVariable Integer examId, Model model, RedirectAttributes redirectAttributes) {
		try {
			model.addAttribute("exam", this.examService.findById(examId));
			model.addAttribute("sectionForm", new AdminSectionForm());
			model.addAttribute("skillTypes", SkillType.values());
			model.addAttribute("formTitle", "Thêm phần thi");
			model.addAttribute("actionUrl", "/admin/exams/" + examId + "/sections");
			return "admin/sections/form";
		} catch (RuntimeException error) {
			redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
			return "redirect:/admin/exams";
		}
	}

	@PostMapping("/admin/exams/{examId}/sections")
	public String create(@PathVariable Integer examId, @ModelAttribute AdminSectionForm form,
			RedirectAttributes redirectAttributes) {
		try {
			Exam exam = this.examService.findById(examId);
			ExamSection section = new ExamSection();
			copyToSection(form, section);
			section.setExam(exam);
			this.examSectionRepository.save(section);
			redirectAttributes.addFlashAttribute("successMessage", "Tạo phần thi thành công.");
		} catch (RuntimeException error) {
			redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
		}
		return "redirect:/admin/exams/" + examId;
	}

	@GetMapping("/admin/sections/{id}/edit")
	public String edit(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
		return this.examSectionRepository.findById(id).map(section -> {
			model.addAttribute("exam", section.getExam());
			model.addAttribute("sectionForm", toForm(section));
			model.addAttribute("skillTypes", SkillType.values());
			model.addAttribute("formTitle", "Chỉnh sửa phần thi");
			model.addAttribute("actionUrl", "/admin/sections/" + id);
			return "admin/sections/form";
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phần thi.");
			return "redirect:/admin/exams";
		});
	}

	@PostMapping("/admin/sections/{id}")
	public String update(@PathVariable Integer id, @ModelAttribute AdminSectionForm form,
			RedirectAttributes redirectAttributes) {
		return this.examSectionRepository.findById(id).map(section -> {
			copyToSection(form, section);
			this.examSectionRepository.save(section);
			redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phần thi thành công.");
			return "redirect:/admin/exams/" + section.getExam().getId();
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phần thi.");
			return "redirect:/admin/exams";
		});
	}

	@PostMapping("/admin/sections/{id}/delete")
	public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
		return this.examSectionRepository.findById(id).map(section -> {
			Integer examId = section.getExam().getId();
			try {
				this.examSectionRepository.delete(section);
				redirectAttributes.addFlashAttribute("successMessage", "Xóa phần thi thành công.");
			} catch (DataIntegrityViolationException error) {
				redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa phần thi vì đã có dữ liệu liên quan.");
			}
			return "redirect:/admin/exams/" + examId;
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phần thi.");
			return "redirect:/admin/exams";
		});
	}

	private AdminSectionForm toForm(ExamSection section) {
		AdminSectionForm form = new AdminSectionForm();
		form.setSkillType(section.getSkillType());
		form.setSectionOrder(section.getSectionOrder());
		form.setPassageContent(section.getPassageContent());
		form.setMediaUrl(section.getMediaUrl());
		return form;
	}

	private void copyToSection(AdminSectionForm form, ExamSection section) {
		section.setSkillType(form.getSkillType());
		section.setSectionOrder(form.getSectionOrder());
		section.setPassageContent(form.getPassageContent());
		section.setMediaUrl(form.getMediaUrl());
	}
}
