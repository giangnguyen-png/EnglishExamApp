package com.englishApp.exam.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
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
	private static final String VIETNAMESE_DIACRITICS_REQUIREMENT = """
			All feedback must be written in natural Vietnamese using full Vietnamese Unicode diacritics.
			Always use Vietnamese tone marks and Vietnamese characters.
			Correct: "Bạn cần cải thiện độ trôi chảy và sử dụng từ vựng đa dạng hơn."
			Incorrect: "Ban can cai thien do troi chay va su dung tu vung da dang hon."
			Never return Vietnamese without diacritics.
			""";

	@Value("${ai.mock-enabled:false}")
	private boolean mockEnabled;

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
		if (mockEnabled) {
			BigDecimal score = taskNumber == 1 ? BigDecimal.valueOf(6.0) : BigDecimal.valueOf(6.5);

			return new AiEvaluationResult(score, new AiFeedback(
					List.of("Bài viết có bố cục tương đối rõ ràng.", "Nội dung có liên quan đến yêu cầu đề bài."),
					List.of("Từ vựng và cấu trúc câu vẫn còn hạn chế.", "Một số ý chưa được phát triển đầy đủ."),
					List.of("Phát triển ý chi tiết hơn.", "Sử dụng đa dạng từ vựng và cấu trúc câu.")));
		}
		return evaluate(buildWritingPrompt(question, answer, taskNumber));
	}

	public AiEvaluationResult evaluateSpeakingAttempt(String speakingTestContent) {
		validateInput(speakingTestContent, "Speaking test content is required");
		if (mockEnabled) {
			return new AiEvaluationResult(BigDecimal.valueOf(6.5),
					new AiFeedback(
							List.of("Câu trả lời nhìn chung đúng trọng tâm.",
									"Có khả năng diễn đạt ý tương đối rõ ràng."),
							List.of("Vốn từ và cấu trúc câu chưa đa dạng.", "Độ trôi chảy cần được cải thiện."),
							List.of("Mở rộng câu trả lời với nhiều chi tiết hơn.",
									"Luyện nói thường xuyên để tăng độ trôi chảy.")));
		}
		return evaluate(buildSpeakingAttemptPrompt(speakingTestContent));
	}

	public AiFeedback evaluateOverall(BigDecimal listeningBand, BigDecimal readingBand, BigDecimal writingBand,
			BigDecimal speakingBand, String writingAiAnalysis, String speakingAiAnalysis) {
		validateBandScore(listeningBand, "Listening band score is required");
		validateBandScore(readingBand, "Reading band score is required");
		validateBandScore(writingBand, "Writing band score is required");
		validateBandScore(speakingBand, "Speaking band score is required");

		if (mockEnabled) {
			return new AiFeedback(
					List.of("Người học có nền tảng tương đối tốt ở một số kỹ năng.",
							"Kết quả bốn kỹ năng tương đối ổn định."),
					List.of("Writing và Speaking vẫn còn khả năng cải thiện."),
					List.of("Tiếp tục luyện tập đều cả bốn kỹ năng.", "Tập trung nhiều hơn vào Writing và Speaking."));
		}

		String json = this.geminiClient.generateJson(buildOverallPrompt(listeningBand, readingBand, writingBand,
				speakingBand, writingAiAnalysis, speakingAiAnalysis), feedbackSchema());
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
				All feedback explanations must be written in Vietnamese.
				%s
				The candidate's IELTS answer remains in English.
				Keep official IELTS criterion names in English when appropriate, for example %s,
				Coherence and Cohesion, Lexical Resource, and Grammatical Range and Accuracy.
				Do not translate JSON property names.
				Do not return markdown or free text.

				Writing task:
				%s

				User answer:
				%s
				"""
				.formatted(taskNumber, taskCriterion, taskNumber, VIETNAMESE_DIACRITICS_REQUIREMENT, taskCriterion,
						safeText(question), answer);
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
				All feedback explanations must be written in Vietnamese.
				%s
				The candidate's IELTS transcript remains in English.
				Keep official IELTS criterion names in English when appropriate, for example Fluency and Coherence,
				Lexical Resource, Grammatical Range and Accuracy, and Pronunciation.
				Do not translate JSON property names.
				Do not return markdown or free text.

				Speaking test content:
				%s
				"""
				.formatted(VIETNAMESE_DIACRITICS_REQUIREMENT, speakingTestContent);
	}

	private String buildOverallPrompt(BigDecimal listeningBand, BigDecimal readingBand, BigDecimal writingBand,
			BigDecimal speakingBand, String writingAiAnalysis, String speakingAiAnalysis) {
		return """
				You are an IELTS coach.
				Create an overall IELTS feedback summary from the candidate's four skill band scores and available AI analyses.

				Return only JSON matching the schema. Do not return markdown or free text.
				Feedback must be short, specific, useful, and suitable for an IELTS learner.
				All feedback explanations must be written in Vietnamese.
				%s
				The candidate's IELTS answer/transcript remains in English.
				Keep official IELTS criterion names in English when appropriate.
				Do not translate JSON property names.

				Band scores:
				Listening: %s
				Reading: %s
				Writing: %s
				Speaking: %s

				Writing AI analysis:
				%s

				Speaking AI analysis:
				%s
				"""
				.formatted(VIETNAMESE_DIACRITICS_REQUIREMENT, listeningBand, readingBand, writingBand, speakingBand, safeText(writingAiAnalysis),
						safeText(speakingAiAnalysis));
	}

	private Schema evaluationSchema() {
		return Schema.builder().type(Type.Known.OBJECT)
				.properties(Map.of("score", Schema.builder().type(Type.Known.NUMBER).minimum(0.0).maximum(9.0)
						.description("One IELTS band score from 0.0 to 9.0, normally in 0.5 increments.").build(),
						"feedback", feedbackSchema()))
				.required("score", "feedback").propertyOrdering("score", "feedback").build();
	}

	private Schema feedbackSchema() {
		Schema stringArray = Schema.builder().type(Type.Known.ARRAY).items(Schema.builder().type(Type.Known.STRING))
				.minItems(1L).maxItems(3L).build();
		return Schema.builder().type(Type.Known.OBJECT)
				.properties(Map.of("strengths", stringArray, "weaknesses", stringArray, "improvements", stringArray))
				.required("strengths", "weaknesses", "improvements")
				.propertyOrdering("strengths", "weaknesses", "improvements").build();
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
		return values == null || values.isEmpty()
				|| values.stream().anyMatch(value -> value == null || value.isBlank());
	}

	private String safeText(String value) {
		return value == null || value.isBlank() ? "Not provided." : value;
	}
}
