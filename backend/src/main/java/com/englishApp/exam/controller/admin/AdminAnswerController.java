package com.englishApp.exam.controller.admin;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.englishApp.exam.dto.admin.AdminAnswerForm;
import com.englishApp.exam.model.Answer;
import com.englishApp.exam.model.Question;
import com.englishApp.exam.repository.AnswerRepository;
import com.englishApp.exam.repository.QuestionRepository;
import com.englishApp.exam.service.QuestionAnswerRules;

@Controller
public class AdminAnswerController {
	private final QuestionRepository questionRepository;
	private final AnswerRepository answerRepository;

	public AdminAnswerController(QuestionRepository questionRepository, AnswerRepository answerRepository) {
		this.questionRepository = questionRepository;
		this.answerRepository = answerRepository;
	}

	@GetMapping("/admin/questions/{questionId}/answers/new")
	public String newAnswer(@PathVariable Integer questionId, Model model, RedirectAttributes redirectAttributes) {
		return this.questionRepository.findById(questionId).map(question -> {
			if (QuestionAnswerRules.doesNotUseChoiceAnswers(question.getQuestionType())) {
				redirectAttributes.addFlashAttribute("errorMessage",
						"Loại câu hỏi này không sử dụng đáp án lựa chọn.");
				return "redirect:/admin/questions/" + questionId;
			}
			model.addAttribute("question", question);
			model.addAttribute("answerForm", new AdminAnswerForm());
			model.addAttribute("formTitle", "Thêm đáp án");
			model.addAttribute("actionUrl", "/admin/questions/" + questionId + "/answers");
			return "admin/answers/form";
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy câu hỏi.");
			return "redirect:/admin/exams";
		});
	}

	@PostMapping("/admin/questions/{questionId}/answers")
	public String create(@PathVariable Integer questionId, @ModelAttribute AdminAnswerForm form,
			RedirectAttributes redirectAttributes) {
		try {
			Question question = this.questionRepository.findById(questionId)
					.orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi."));
			validateAnswerAllowed(question);
			validateAnswerState(question, null, form.isCorrect());
			Answer answer = new Answer();
			copyToAnswer(form, answer);
			answer.setQuestion(question);
			this.answerRepository.save(answer);
			redirectAttributes.addFlashAttribute("successMessage", "Tạo đáp án thành công.");
		} catch (RuntimeException error) {
			redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
		}
		return "redirect:/admin/questions/" + questionId;
	}

	@GetMapping("/admin/answers/{id}/edit")
	public String edit(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
		return this.answerRepository.findById(id).map(answer -> {
			if (QuestionAnswerRules.doesNotUseChoiceAnswers(answer.getQuestion().getQuestionType())) {
				redirectAttributes.addFlashAttribute("errorMessage",
						"Loại câu hỏi này không sử dụng đáp án lựa chọn.");
				return "redirect:/admin/questions/" + answer.getQuestion().getId();
			}
			model.addAttribute("question", answer.getQuestion());
			model.addAttribute("answerForm", toForm(answer));
			model.addAttribute("formTitle", "Chỉnh sửa đáp án");
			model.addAttribute("actionUrl", "/admin/answers/" + id);
			return "admin/answers/form";
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đáp án.");
			return "redirect:/admin/exams";
		});
	}

	@PostMapping("/admin/answers/{id}")
	public String update(@PathVariable Integer id, @ModelAttribute AdminAnswerForm form,
			RedirectAttributes redirectAttributes) {
		return this.answerRepository.findById(id).map(answer -> {
			try {
				validateAnswerAllowed(answer.getQuestion());
				validateAnswerState(answer.getQuestion(), answer.getId(), form.isCorrect());
				copyToAnswer(form, answer);
				this.answerRepository.save(answer);
				redirectAttributes.addFlashAttribute("successMessage", "Cập nhật đáp án thành công.");
			} catch (RuntimeException error) {
				redirectAttributes.addFlashAttribute("errorMessage", error.getMessage());
			}
			return "redirect:/admin/questions/" + answer.getQuestion().getId();
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đáp án.");
			return "redirect:/admin/exams";
		});
	}

	@PostMapping("/admin/answers/{id}/delete")
	public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
		return this.answerRepository.findById(id).map(answer -> {
			Integer questionId = answer.getQuestion().getId();
			try {
				this.answerRepository.delete(answer);
				redirectAttributes.addFlashAttribute("successMessage", "Xóa đáp án thành công.");
			} catch (DataIntegrityViolationException error) {
				redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa đáp án vì đã có câu trả lời của thí sinh.");
			}
			return "redirect:/admin/questions/" + questionId;
		}).orElseGet(() -> {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đáp án.");
			return "redirect:/admin/exams";
		});
	}

	private AdminAnswerForm toForm(Answer answer) {
		AdminAnswerForm form = new AdminAnswerForm();
		form.setContent(answer.getContent());
		form.setCorrect(answer.isCorrect());
		form.setExplanation(answer.getExplanation());
		return form;
	}

	private void copyToAnswer(AdminAnswerForm form, Answer answer) {
		answer.setContent(form.getContent());
		answer.setCorrect(form.isCorrect());
		answer.setExplanation(form.getExplanation());
	}

	private void validateAnswerAllowed(Question question) {
		if (QuestionAnswerRules.doesNotUseChoiceAnswers(question.getQuestionType())) {
			throw new RuntimeException("Loại câu hỏi này không sử dụng đáp án lựa chọn.");
		}
	}

	private void validateAnswerState(Question question, Integer currentAnswerId, boolean submittedCorrect) {
		long correctCount = this.answerRepository.findByQuestionId(question.getId()).stream()
				.filter(answer -> currentAnswerId == null || !answer.getId().equals(currentAnswerId))
				.filter(Answer::isCorrect)
				.count();

		if (QuestionAnswerRules.isSingleCorrectChoice(question.getQuestionType()) && submittedCorrect
				&& correctCount >= 1) {
			throw new RuntimeException("Câu hỏi này chỉ được có một đáp án đúng.");
		}
	}
}
