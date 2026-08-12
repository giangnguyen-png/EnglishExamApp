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

	public AiEvaluationResult evaluateWriting(String question, String answer) {
		validateInput(answer, "Writing answer is required");
		return evaluate(buildWritingPrompt(question, answer));
	}

	public AiEvaluationResult evaluateSpeaking(String question, String transcript) {
		validateInput(transcript, "Speaking transcript is required");
		return evaluate(buildSpeakingPrompt(question, transcript));
	}

	private AiEvaluationResult evaluate(String prompt) {
		String json = this.geminiClient.generateJson(prompt, evaluationSchema());
		AiEvaluationResult result = parseEvaluation(json);
		validateResult(result);
		return result;
	}

	private String buildWritingPrompt(String question, String answer) {
		return """
				You are an IELTS Writing examiner.
				Evaluate the user's writing response using these IELTS Writing criteria:
				Task Response, Coherence and Cohesion, Lexical Resource, and Grammatical Range and Accuracy.

				Return only JSON matching the schema. Give one overall response score from 0.0 to 9.0.
				Feedback must be short, specific, useful, and based on the submitted answer.

				Writing task:
				%s

				User answer:
				%s
				""".formatted(safeText(question), answer);
	}

	private String buildSpeakingPrompt(String question, String transcript) {
		return """
				You are an IELTS Speaking examiner.
				Evaluate the user's speaking response from the available transcript using:
				Fluency and Coherence, Lexical Resource, and Grammatical Range and Accuracy.
				Do not claim to accurately assess Pronunciation because no audio analysis is provided.

				Return only JSON matching the schema. Give one overall response score from 0.0 to 9.0.
				Feedback must be short, specific, useful, and based on the transcript.

				Speaking prompt:
				%s

				Speech-to-text transcript:
				%s
				""".formatted(safeText(question), transcript);
	}

	private Schema evaluationSchema() {
		return Schema.builder()
				.type(Type.Known.OBJECT)
				.properties(Map.of(
						"score", Schema.builder()
								.type(Type.Known.NUMBER)
								.minimum(0.0)
								.maximum(9.0)
								.description("One IELTS band score for this single response.")
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

	private boolean isBlank(List<String> values) {
		return values == null || values.isEmpty() || values.stream().anyMatch(value -> value == null || value.isBlank());
	}

	private String safeText(String value) {
		return value == null || value.isBlank() ? "Not provided." : value;
	}
}
