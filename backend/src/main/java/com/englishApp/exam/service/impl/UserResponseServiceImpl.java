package com.englishApp.exam.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.englishApp.exam.dto.ai.AiEvaluationResult;
import com.englishApp.exam.dto.cloudinary.CloudinaryUploadResult;
import com.englishApp.exam.model.Answer;
import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.model.Question;
import com.englishApp.exam.model.SkillResult;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.UserResponse;
import com.englishApp.exam.model.UserResponseChoice;
import com.englishApp.exam.model.enums.MockSessionStatus;
import com.englishApp.exam.model.enums.SkillType;
import com.englishApp.exam.repository.AnswerRepository;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.QuestionRepository;
import com.englishApp.exam.repository.SkillResultRepository;
import com.englishApp.exam.repository.TestAttemptRepository;
import com.englishApp.exam.repository.UserResponseChoiceRepository;
import com.englishApp.exam.repository.UserResponseRepository;
import com.englishApp.exam.service.AiService;
import com.englishApp.exam.service.CloudinaryService;
import com.englishApp.exam.service.SpeechService;
import com.englishApp.exam.service.UserResponseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserResponseServiceImpl implements UserResponseService {
	private final UserResponseRepository userResponseRepository;
	private final UserResponseChoiceRepository userResponseChoiceRepository;
	private final TestAttemptRepository testAttemptRepository;
	private final QuestionRepository questionRepository;
	private final AnswerRepository answerRepository;
	private final SkillResultRepository skillResultRepository;
	private final MockSessionRepository mockSessionRepository;
	private final AiService aiService;
	private final SpeechService speechService;
	private final CloudinaryService cloudinaryService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public UserResponseServiceImpl(UserResponseRepository userResponseRepository,
			UserResponseChoiceRepository userResponseChoiceRepository, TestAttemptRepository testAttemptRepository,
			QuestionRepository questionRepository, AnswerRepository answerRepository,
			SkillResultRepository skillResultRepository, MockSessionRepository mockSessionRepository,
			AiService aiService, SpeechService speechService, CloudinaryService cloudinaryService) {
		this.userResponseRepository = userResponseRepository;
		this.userResponseChoiceRepository = userResponseChoiceRepository;
		this.testAttemptRepository = testAttemptRepository;
		this.questionRepository = questionRepository;
		this.answerRepository = answerRepository;
		this.skillResultRepository = skillResultRepository;
		this.mockSessionRepository = mockSessionRepository;
		this.aiService = aiService;
		this.speechService = speechService;
		this.cloudinaryService = cloudinaryService;
	}

	@Transactional
	public UserResponse saveAnswer(Integer attemptId, Integer questionId, List<Integer> answerIds) {
		UserResponse response = getOrCreateResponse(attemptId, questionId);
		validateAnswerChoiceSkill(response);
		UserResponse savedResponse = this.userResponseRepository.save(response);
		this.userResponseChoiceRepository
				.deleteAll(this.userResponseChoiceRepository.findByResponseId(savedResponse.getId()));
		userResponseChoiceRepository.flush();
		if (answerIds != null) {
			for (Integer answerId : answerIds) {
				Answer answer = this.answerRepository.findById(answerId)
						.orElseThrow(() -> new RuntimeException("Answer not found"));
				if (!answer.getQuestion().getId().equals(questionId)) {
					throw new RuntimeException("Answer does not belong to question");
				}
				UserResponseChoice choice = new UserResponseChoice();
				choice.setResponse(savedResponse);
				choice.setAnswer(answer);
				this.userResponseChoiceRepository.save(choice);
			}
		}
		return savedResponse;
	}

	@Transactional
	public UserResponse submitWriting(Integer attemptId, Integer questionId, String textContent) {
		if (textContent == null || textContent.isBlank()) {
			throw new RuntimeException("Writing content is required");
		}
		return gradeWritingResponse(saveWritingDraft(attemptId, questionId, textContent));
	}

	@Transactional
	public UserResponse saveWritingDraft(Integer attemptId, Integer questionId, String textContent) {
		UserResponse response = getOrCreateResponse(attemptId, questionId);
		validateSkill(response, SkillType.WRITING);
		response.setTextContent(textContent);
		return this.userResponseRepository.save(response);
	}

	private UserResponse gradeWritingResponse(UserResponse response) {
		AiEvaluationResult evaluation = this.aiService.evaluateWritingTask(response.getQuestion().getContent(),
				response.getTextContent(), determineWritingTaskNumber(response.getQuestion()));
		BigDecimal normalizedScore = roundToHalfBand(evaluation.score().doubleValue());
		AiEvaluationResult normalizedEvaluation = new AiEvaluationResult(normalizedScore, evaluation.feedback());
		response.setAiScore(normalizedScore);
		UserResponse savedResponse = this.userResponseRepository.save(response);
		saveWritingAnalysis(savedResponse, normalizedEvaluation);
		return savedResponse;
	}

	@Transactional
	public UserResponse submitSpeaking(Integer attemptId, Integer questionId, MultipartFile audioFile) {
		if (audioFile == null || audioFile.isEmpty()) {
			throw new RuntimeException("Speaking audio is required");
		}
		UserResponse response = getOrCreateResponse(attemptId, questionId);
		validateSkill(response, SkillType.SPEAKING);
		byte[] audioData = readAudioBytes(audioFile);
		CloudinaryUploadResult uploadResult = this.cloudinaryService.uploadAudio(audioFile);
		String transcript = null;
		try {
			transcript = this.speechService.speechToText(audioData);
		} catch (RuntimeException error) {
			if (response.getAttempt().getSession() == null) {
				throw error;
			}
		}
		response.setFileUrl(uploadResult.secureUrl());
		response.setFilePublicId(uploadResult.publicId());
		response.setSpeechToTextTrans(transcript);
		response.setAiScore(null);
		return this.userResponseRepository.save(response);
	}

	public List<UserResponse> findByAttempt(Integer attemptId) {
		if (!this.testAttemptRepository.existsById(attemptId)) {
			throw new RuntimeException("Test attempt not found");
		}
		return this.userResponseRepository.findByAttemptId(attemptId);
	}

	public List<UserResponse> findSpeakingResponsesBySession(Integer sessionId) {
		if (!this.mockSessionRepository.existsById(sessionId)) {
			throw new RuntimeException("Session not found");
		}
		return this.userResponseRepository
				.findByAttemptSessionIdAndAttemptEndTimeIsNotNullAndQuestionExamSectionSkillType(sessionId,
						SkillType.SPEAKING);
	}

	private UserResponse getOrCreateResponse(Integer attemptId, Integer questionId) {
		TestAttempt attempt = this.testAttemptRepository.findByIdForUpdate(attemptId)
				.orElseThrow(() -> new RuntimeException("Test attempt not found"));
		if (attempt.getEndTime() != null) {
			throw new RuntimeException("Test attempt has already been submitted");
		}
		validateOpenAttempt(attempt);
		Question question = this.questionRepository.findById(questionId)
				.orElseThrow(() -> new RuntimeException("Question not found"));
		if (!question.getExamSection().getExam().getId().equals(attempt.getExam().getId())) {
			throw new RuntimeException("Question does not belong to this exam");
		}
		return this.userResponseRepository.findByAttemptIdAndQuestionId(attemptId, questionId).orElseGet(() -> {
			UserResponse response = new UserResponse();
			response.setAttempt(attempt);
			response.setQuestion(question);
			return response;
		});
	}

	private void validateOpenAttempt(TestAttempt attempt) {
		MockSession session = attempt.getSession();
		if (session == null) {
			return;
		}
		if (session.getStatus() != MockSessionStatus.ONGOING) {
			throw new RuntimeException("Ca thi đã kết thúc.");
		}
		if (session.getEndTime() != null && !java.time.LocalDateTime.now().isBefore(session.getEndTime())) {
			throw new RuntimeException("Ca thi đã kết thúc.");
		}
	}

	private byte[] readAudioBytes(MultipartFile audioFile) {
		try {
			return audioFile.getBytes();
		} catch (IOException e) {
			throw new RuntimeException("Could not read speaking audio", e);
		}
	}

	private void validateSkill(UserResponse response, SkillType expectedSkill) {
		if (response.getQuestion().getExamSection().getSkillType() != expectedSkill) {
			throw new RuntimeException("Question does not belong to " + expectedSkill);
		}
	}

	private void validateAnswerChoiceSkill(UserResponse response) {
		SkillType skillType = response.getQuestion().getExamSection().getSkillType();
		if (skillType == SkillType.WRITING || skillType == SkillType.SPEAKING) {
			throw new RuntimeException("Question does not support answer choices");
		}
	}

	private int determineWritingTaskNumber(Question question) {
		List<Question> writingQuestions = this.questionRepository
				.findByExamSectionExamIdAndExamSectionSkillTypeOrderByExamSectionSectionOrderAscOrderIndexAsc(
						question.getExamSection().getExam().getId(), SkillType.WRITING);
		for (int i = 0; i < writingQuestions.size(); i++) {
			if (writingQuestions.get(i).getId().equals(question.getId())) {
				return i == 0 ? 1 : 2;
			}
		}
		throw new RuntimeException("Writing question not found in this exam");
	}

	private void saveWritingAnalysis(UserResponse response, AiEvaluationResult evaluation) {
		SkillResult result = this.skillResultRepository
				.findByAttemptIdAndSkillType(response.getAttempt().getId(), SkillType.WRITING)
				.orElseGet(SkillResult::new);
		result.setAttempt(response.getAttempt());
		result.setSkillType(SkillType.WRITING);
		List<Map<String, Object>> entries = readWritingAnalysis(result.getAiAnalysis());
		entries.removeIf(entry -> response.getId().equals(entry.get("responseId")));
		entries.add(Map.of("responseId", response.getId(), "questionId", response.getQuestion().getId(), "score",
				evaluation.score(), "feedback", evaluation.feedback()));
		result.setAiAnalysis(writeWritingAnalysis(entries));
		this.skillResultRepository.save(result);
	}

	private List<Map<String, Object>> readWritingAnalysis(String analysis) {
		if (analysis == null || analysis.isBlank()) {
			return new ArrayList<>();
		}
		try {
			return new ArrayList<>(
					this.objectMapper.readValue(analysis, new TypeReference<List<Map<String, Object>>>() {
					}));
		} catch (JsonProcessingException e) {
			return new ArrayList<>();
		}
	}

	private String writeWritingAnalysis(List<Map<String, Object>> entries) {
		try {
			return this.objectMapper.writeValueAsString(entries);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize AI analysis", e);
		}
	}

	private BigDecimal roundToHalfBand(double value) {
		return BigDecimal.valueOf(Math.round(value * 2.0) / 2.0).setScale(1, RoundingMode.HALF_UP);
	}
}
