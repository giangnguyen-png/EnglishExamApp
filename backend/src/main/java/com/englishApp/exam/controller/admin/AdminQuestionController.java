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

import com.englishApp.exam.dto.admin.AdminQuestionForm;
import com.englishApp.exam.model.ExamSection;
import com.englishApp.exam.model.Question;
import com.englishApp.exam.model.enums.QuestionType;
import com.englishApp.exam.repository.AnswerRepository;
import com.englishApp.exam.repository.ExamSectionRepository;
import com.englishApp.exam.repository.QuestionRepository;
import com.englishApp.exam.service.QuestionAnswerRules;

@Controller
public class AdminQuestionController {
	private final ExamSectionRepository examSectionRepository;
	private final QuestionRepository questionRepository;
	private final AnswerRepository answerRepository;

	public AdminQuestionController(ExamSectionRepository examSectionRepository, QuestionRepository questionRepository,
			AnswerRepository answerRepository) {
		this.examSectionRepository = examSectionRepository;
		this.questionRepository = questionRepository;
		this.answerRepository = answerRepository;
	}

	@GetMapping("/admin/sections/{sectionId}/questions")
	public String list(@PathVariable Integer sectionId, Model model, RedirectAttributes redirectAttributes) {
		return this.examSectionRepository.findById(sectionId).map(section -> {
			model.addAttribute("section", section);
			model.addAttribute("questions", this.questionRepository.findByExamSectionIdOrderByOrderIndex(sectionId));
			return "admin/questions/list";
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phần thi.");
			return "redirect:/admin/exams";
		});
	}

	@GetMapping("/admin/sections/{sectionId}/questions/new")
	public String newQuestion(@PathVariable Integer sectionId, Model model, RedirectAttributes redirectAttributes) {
		return this.examSectionRepository.findById(sectionId).map(section -> {
			model.addAttribute("section", section);
			model.addAttribute("questionForm", new AdminQuestionForm());
			model.addAttribute("questionTypes", QuestionType.values());
			model.addAttribute("formTitle", "Thêm câu hỏi");
			model.addAttribute("actionUrl", "/admin/sections/" + sectionId + "/questions");
			return "admin/questions/form";
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phần thi.");
			return "redirect:/admin/exams";
		});
	}

	@PostMapping("/admin/sections/{sectionId}/questions")
	public String create(@PathVariable Integer sectionId, @ModelAttribute AdminQuestionForm form,
			RedirectAttributes redirectAttributes) {
		try {
			ExamSection section = this.examSectionRepository.findById(sectionId)
					.orElseThrow(() -> new RuntimeException("Không tìm thấy phần thi."));
			Question question = new Question();
			copyToQuestion(form, question);
			question.setExamSection(section);
			Question savedQuestion = this.questionRepository.save(question);
			redirectAttributes.addFlashAttribute("successMessage", "Tạo câu hỏi thành công.");
			return "redirect:/admin/questions/" + savedQuestion.getId();
		} catch (RuntimeException error) {
			redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
			return "redirect:/admin/sections/" + sectionId + "/questions/new";
		}
	}

	@GetMapping("/admin/questions/{id}")
	public String detail(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
		return this.questionRepository.findById(id).map(question -> {
			boolean canUseAnswers = QuestionAnswerRules.usesChoiceAnswers(question.getQuestionType());
			model.addAttribute("question", question);
			model.addAttribute("canUseAnswers", canUseAnswers);
			model.addAttribute("answers", this.answerRepository.findByQuestionId(id).stream()
					.sorted(Comparator.comparing(answer -> answer.getId())).toList());
			return "admin/questions/detail";
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy câu hỏi.");
			return "redirect:/admin/exams";
		});
	}

	@GetMapping("/admin/questions/{id}/edit")
	public String edit(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
		return this.questionRepository.findById(id).map(question -> {
			model.addAttribute("section", question.getExamSection());
			model.addAttribute("questionForm", toForm(question));
			model.addAttribute("questionTypes", QuestionType.values());
			model.addAttribute("formTitle", "Chỉnh sửa câu hỏi");
			model.addAttribute("actionUrl", "/admin/questions/" + id);
			return "admin/questions/form";
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy câu hỏi.");
			return "redirect:/admin/exams";
		});
	}

	@PostMapping("/admin/questions/{id}")
	public String update(@PathVariable Integer id, @ModelAttribute AdminQuestionForm form,
			RedirectAttributes redirectAttributes) {
		return this.questionRepository.findById(id).map(question -> {
			try {
				validateQuestionTypeChange(question, form.getQuestionType());
				copyToQuestion(form, question);
				this.questionRepository.save(question);
				redirectAttributes.addFlashAttribute("successMessage", "Cập nhật câu hỏi thành công.");
			} catch (RuntimeException error) {
				redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
			}
			return "redirect:/admin/questions/" + id;
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy câu hỏi.");
			return "redirect:/admin/exams";
		});
	}

	@PostMapping("/admin/questions/{id}/delete")
	public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
		return this.questionRepository.findById(id).map(question -> {
			Integer sectionId = question.getExamSection().getId();
			try {
				this.questionRepository.delete(question);
				redirectAttributes.addFlashAttribute("successMessage", "Xóa câu hỏi thành công.");
			} catch (DataIntegrityViolationException error) {
				redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa câu hỏi vì đã có câu trả lời của thí sinh.");
			}
			return "redirect:/admin/sections/" + sectionId + "/questions";
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy câu hỏi.");
			return "redirect:/admin/exams";
		});
	}

	private AdminQuestionForm toForm(Question question) {
		AdminQuestionForm form = new AdminQuestionForm();
		form.setQuestionType(question.getQuestionType());
		form.setContent(question.getContent());
		form.setPoints(question.getPoints());
		form.setOrderIndex(question.getOrderIndex());
		form.setImageUrl(question.getImageUrl());
		form.setDurationSeconds(question.getDurationSeconds());
		form.setPreparationSeconds(question.getPreparationSeconds());
		return form;
	}

	private void copyToQuestion(AdminQuestionForm form, Question question) {
		question.setQuestionType(form.getQuestionType());
		question.setContent(form.getContent());
		question.setPoints(form.getPoints());
		question.setOrderIndex(form.getOrderIndex());
		question.setImageUrl(blankToNull(form.getImageUrl()));
		question.setDurationSeconds(form.getDurationSeconds());
		question.setPreparationSeconds(form.getPreparationSeconds());
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private void validateQuestionTypeChange(Question question, QuestionType nextType) {
		if (QuestionAnswerRules.doesNotUseChoiceAnswers(nextType)
				&& !this.answerRepository.findByQuestionId(question.getId()).isEmpty()) {
			throw new RuntimeException("Loại câu hỏi này không sử dụng đáp án lựa chọn.");
		}
	}
}
