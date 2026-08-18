package com.englishApp.exam.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.englishApp.exam.client.GeminiClient;
import com.englishApp.exam.dto.ai.AiEvaluationResult;
import com.englishApp.exam.dto.ai.AiFeedback;
import com.englishApp.exam.service.AiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;

@Service
public class AiServiceImpl implements AiService {
	private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
	private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(9.0);

	private final GeminiClient geminiClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public AiServiceImpl(GeminiClient geminiClient) {
		this.geminiClient = geminiClient;
	}

	public AiEvaluationResult evaluateWritingTask(String question, String answer, int taskNumber) {
		validateInput(answer, "Writing answer is required");
		if (taskNumber != 1 && taskNumber != 2) {
			throw new IllegalArgumentException("Writing task number must be 1 or 2");
		}
		return evaluate(buildWritingPrompt(question, answer, taskNumber));
	}

	public AiEvaluationResult evaluateSpeakingAttempt(String speakingTestContent) {
		validateInput(speakingTestContent, "Speaking test content is required");
		return evaluate(buildSpeakingAttemptPrompt(speakingTestContent));
	}

	public AiFeedback evaluateOverall(BigDecimal listeningBand, BigDecimal readingBand, BigDecimal writingBand,
			BigDecimal speakingBand, String writingAiAnalysis, String speakingAiAnalysis) {
		validateBandScore(listeningBand, "Listening band score is required");
		validateBandScore(readingBand, "Reading band score is required");
		validateBandScore(writingBand, "Writing band score is required");
		validateBandScore(speakingBand, "Speaking band score is required");

		String json = this.geminiClient.generateJson(
				buildOverallPrompt(listeningBand, readingBand, writingBand, speakingBand, writingAiAnalysis,
						speakingAiAnalysis),
				feedbackSchema());
		AiFeedback feedback = parseFeedback(json);
		validateFeedback(feedback);
		return feedback;
	}

	private AiEvaluationResult evaluate(String prompt) {
		String json = this.geminiClient.generateJson(prompt, evaluationSchema());
		AiEvaluationResult result = parseEvaluation(json);
		validateResult(result);
		return result;
	}

	private String buildWritingPrompt(String question, String answer, int taskNumber) {
		String taskCriterion = taskNumber == 1 ? "Task Achievement" : "Task Response";
		return """
				You are an IELTS Writing examiner.
				Evaluate this IELTS Academic Writing Task %d response using these IELTS Writing criteria:
				%s, Coherence and Cohesion, Lexical Resource, and Grammatical Range and Accuracy.

				Return only JSON matching the schema. Give one overall Task %d band score from 0.0 to 9.0 in 0.5 increments.
				Feedback must be short, specific, useful, and based on the submitted answer.
				Do not return markdown or free text.

				Writing task:
				%s

				User answer:
				%s
				""".formatted(taskNumber, taskCriterion, taskNumber, safeText(question), answer);
	}

	private String buildSpeakingAttemptPrompt(String speakingTestContent) {
		return """
				You are an IELTS Speaking examiner.
				Evaluate the candidate's overall IELTS Speaking performance from the full speech-to-text transcript set.

				Use these IELTS Speaking criteria:
				Fluency and Coherence, Lexical Resource, Grammatical Range and Accuracy, and Pronunciation.

				Important limitation:
				Pronunciation cannot be fully or accurately assessed from transcript text alone because no audio analysis is provided.
				Do not invent specific pronunciation errors.
				Give an estimated Speaking band based mainly on Fluency and Coherence that can be inferred from transcript continuity,
				Lexical Resource, Grammatical Range and Accuracy, relevance/completeness of answers, and any unanswered questions.

				Return only JSON matching the schema. Give one overall Speaking band score from 0.0 to 9.0 in 0.5 increments.
				Feedback must be short, specific, useful, and must mention that the score is an estimate from transcripts.
				Do not return markdown or free text.

				Speaking test content:
				%s
				""".formatted(speakingTestContent);
	}

	private String buildOverallPrompt(BigDecimal listeningBand, BigDecimal readingBand, BigDecimal writingBand,
			BigDecimal speakingBand, String writingAiAnalysis, String speakingAiAnalysis) {
		return """
				You are an IELTS coach.
				Create an overall IELTS feedback summary from the candidate's four skill band scores and available AI analyses.

				Return only JSON matching the schema. Do not return markdown or free text.
				Feedback must be short, specific, useful, and suitable for an IELTS learner.

				Band scores:
				Listening: %s
				Reading: %s
				Writing: %s
				Speaking: %s

				Writing AI analysis:
				%s

				Speaking AI analysis:
				%s
				""".formatted(listeningBand, readingBand, writingBand, speakingBand, safeText(writingAiAnalysis),
				safeText(speakingAiAnalysis));
	}

	private Schema evaluationSchema() {
		return Schema.builder()
				.type(Type.Known.OBJECT)
				.properties(Map.of(
						"score", Schema.builder()
								.type(Type.Known.NUMBER)
								.minimum(0.0)
								.maximum(9.0)
								.description("One IELTS band score from 0.0 to 9.0, normally in 0.5 increments.")
								.build(),
						"feedback", feedbackSchema()))
				.required("score", "feedback")
				.propertyOrdering("score", "feedback")
				.build();
	}

	private Schema feedbackSchema() {
		Schema stringArray = Schema.builder()
				.type(Type.Known.ARRAY)
				.items(Schema.builder().type(Type.Known.STRING))
				.minItems(1L)
				.maxItems(3L)
				.build();
		return Schema.builder()
				.type(Type.Known.OBJECT)
				.properties(Map.of(
						"strengths", stringArray,
						"weaknesses", stringArray,
						"improvements", stringArray))
				.required("strengths", "weaknesses", "improvements")
				.propertyOrdering("strengths", "weaknesses", "improvements")
				.build();
	}

	private AiEvaluationResult parseEvaluation(String json) {
		try {
			return this.objectMapper.readValue(json, AiEvaluationResult.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Gemini returned invalid evaluation JSON", e);
		}
	}

	private AiFeedback parseFeedback(String json) {
		try {
			return this.objectMapper.readValue(json, AiFeedback.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Gemini returned invalid feedback JSON", e);
		}
	}

	private void validateResult(AiEvaluationResult result) {
		if (result == null || result.score() == null) {
			throw new IllegalStateException("Gemini evaluation is missing score");
		}
		if (result.score().compareTo(MIN_SCORE) < 0 || result.score().compareTo(MAX_SCORE) > 0) {
			throw new IllegalStateException("Gemini score is outside the valid IELTS range");
		}
		validateFeedback(result.feedback());
	}

	private void validateFeedback(AiFeedback feedback) {
		if (feedback == null || isBlank(feedback.strengths()) || isBlank(feedback.weaknesses())
				|| isBlank(feedback.improvements())) {
			throw new IllegalStateException("Gemini feedback is missing required sections");
		}
	}

	private void validateInput(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
	}

	private void validateBandScore(BigDecimal score, String message) {
		if (score == null) {
			throw new IllegalArgumentException(message);
		}
		if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
			throw new IllegalArgumentException("IELTS band score is outside the valid range");
		}
	}

	private boolean isBlank(List<String> values) {
		return values == null || values.isEmpty() || values.stream().anyMatch(value -> value == null || value.isBlank());
	}

	private String safeText(String value) {
		return value == null || value.isBlank() ? "Not provided." : value;
	}
}
