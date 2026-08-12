package com.englishApp.exam.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.englishApp.exam.dto.ai.AiEvaluationResult;
import com.englishApp.exam.model.Answer;
import com.englishApp.exam.model.Question;
import com.englishApp.exam.model.SkillResult;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.UserResponse;
import com.englishApp.exam.model.UserResponseChoice;
import com.englishApp.exam.model.enums.SkillType;
import com.englishApp.exam.dto.cloudinary.CloudinaryUploadResult;
import com.englishApp.exam.repository.AnswerRepository;
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
	private final AiService aiService;
	private final SpeechService speechService;
	private final CloudinaryService cloudinaryService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public UserResponseServiceImpl(UserResponseRepository userResponseRepository,
			UserResponseChoiceRepository userResponseChoiceRepository, TestAttemptRepository testAttemptRepository,
			QuestionRepository questionRepository, AnswerRepository answerRepository,
			SkillResultRepository skillResultRepository, AiService aiService, SpeechService speechService,
			CloudinaryService cloudinaryService) {
		this.userResponseRepository = userResponseRepository;
		this.userResponseChoiceRepository = userResponseChoiceRepository;
		this.testAttemptRepository = testAttemptRepository;
		this.questionRepository = questionRepository;
		this.answerRepository = answerRepository;
		this.skillResultRepository = skillResultRepository;
		this.aiService = aiService;
		this.speechService = speechService;
		this.cloudinaryService = cloudinaryService;
	}

	@Transactional
	public UserResponse saveAnswer(Integer attemptId, Integer questionId, List<Integer> answerIds) {
		UserResponse response = getOrCreateResponse(attemptId, questionId);
		UserResponse savedResponse = this.userResponseRepository.save(response);
		this.userResponseChoiceRepository
				.deleteAll(this.userResponseChoiceRepository.findByResponseId(savedResponse.getId()));
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

	public UserResponse submitWriting(Integer attemptId, Integer questionId, String textContent) {
		if (textContent == null || textContent.isBlank()) {
			throw new RuntimeException("Writing content is required");
		}
		UserResponse response = getOrCreateResponse(attemptId, questionId);
		response.setTextContent(textContent);
		AiEvaluationResult evaluation = this.aiService.evaluateWriting(response.getQuestion().getContent(), textContent);
		response.setAiScore(evaluation.score());
		UserResponse savedResponse = this.userResponseRepository.save(response);
		saveAiAnalysis(savedResponse, SkillType.WRITING, evaluation);
		return savedResponse;
	}

	public UserResponse submitSpeaking(Integer attemptId, Integer questionId, MultipartFile audioFile) {
		if (audioFile == null || audioFile.isEmpty()) {
			throw new RuntimeException("Speaking audio is required");
		}
		UserResponse response = getOrCreateResponse(attemptId, questionId);
		byte[] audioData = readAudioBytes(audioFile);
		CloudinaryUploadResult uploadResult = this.cloudinaryService.uploadAudio(audioFile);
		String transcript = this.speechService.speechToText(audioData);
		response.setFileUrl(uploadResult.secureUrl());
		response.setFilePublicId(uploadResult.publicId());
		response.setSpeechToTextTrans(transcript);
		AiEvaluationResult evaluation = this.aiService.evaluateSpeaking(response.getQuestion().getContent(), transcript);
		response.setAiScore(evaluation.score());
		UserResponse savedResponse = this.userResponseRepository.save(response);
		saveAiAnalysis(savedResponse, SkillType.SPEAKING, evaluation);
		return savedResponse;
	}

	public List<UserResponse> findByAttempt(Integer attemptId) {
		if (!this.testAttemptRepository.existsById(attemptId)) {
			throw new RuntimeException("Test attempt not found");
		}
		return this.userResponseRepository.findByAttemptId(attemptId);
	}

	private UserResponse getOrCreateResponse(Integer attemptId, Integer questionId) {
		TestAttempt attempt = this.testAttemptRepository.findById(attemptId)
				.orElseThrow(() -> new RuntimeException("Test attempt not found"));
		Question question = this.questionRepository.findById(questionId)
				.orElseThrow(() -> new RuntimeException("Question not found"));
		return this.userResponseRepository.findByAttemptIdAndQuestionId(attemptId, questionId).orElseGet(() -> {
			UserResponse response = new UserResponse();
			response.setAttempt(attempt);
			response.setQuestion(question);
			return response;
		});
	}

	private byte[] readAudioBytes(MultipartFile audioFile) {
		try {
			return audioFile.getBytes();
		} catch (IOException e) {
			throw new RuntimeException("Could not read speaking audio", e);
		}
	}

	private void saveAiAnalysis(UserResponse response, SkillType skillType, AiEvaluationResult evaluation) {
		SkillResult result = this.skillResultRepository
				.findByAttemptIdAndSkillType(response.getAttempt().getId(), skillType)
				.orElseGet(SkillResult::new);
		result.setAttempt(response.getAttempt());
		result.setSkillType(skillType);
		result.setAiAnalysis(writeAiAnalysis(mergeAiAnalysis(result.getAiAnalysis(), response, evaluation)));
		this.skillResultRepository.save(result);
	}

	private List<Map<String, Object>> mergeAiAnalysis(String currentAnalysis, UserResponse response,
			AiEvaluationResult evaluation) {
		List<Map<String, Object>> entries = readAiAnalysis(currentAnalysis);
		entries.removeIf(entry -> response.getId().equals(entry.get("responseId")));
		entries.add(Map.of(
				"responseId", response.getId(),
				"questionId", response.getQuestion().getId(),
				"score", evaluation.score(),
				"feedback", evaluation.feedback()));
		return entries;
	}

	private List<Map<String, Object>> readAiAnalysis(String analysis) {
		if (analysis == null || analysis.isBlank()) {
			return new ArrayList<>();
		}
		try {
			return new ArrayList<>(this.objectMapper.readValue(analysis, new TypeReference<List<Map<String, Object>>>() {
			}));
		} catch (JsonProcessingException e) {
			return new ArrayList<>();
		}
	}

	private String writeAiAnalysis(List<Map<String, Object>> entries) {
		try {
			return this.objectMapper.writeValueAsString(entries);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize AI analysis", e);
		}
	}
}
