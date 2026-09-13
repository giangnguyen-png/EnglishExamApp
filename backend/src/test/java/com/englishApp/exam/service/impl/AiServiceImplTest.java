package com.englishApp.exam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.englishApp.exam.client.GeminiClient;
import com.englishApp.exam.dto.ai.AiEvaluationResult;
import com.englishApp.exam.dto.ai.AiFeedback;
import com.google.genai.types.Schema;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {
	@Mock
	private GeminiClient geminiClient;

	private AiServiceImpl service;

	@BeforeEach
	void setUp() {
		this.service = new AiServiceImpl(this.geminiClient);
	}

	@Test
	void evaluateWritingTask_whenGeminiReturnsValidJson_shouldReturnParsedEvaluation() {
		when(this.geminiClient.generateJson(any(String.class), any(Schema.class))).thenReturn("""
				{
				  "score": 6.5,
				  "feedback": {
				    "strengths": ["Bài viết trả lời đúng trọng tâm."],
				    "weaknesses": ["Một số câu còn thiếu chính xác."],
				    "improvements": ["Phát triển luận điểm rõ hơn."]
				  }
				}
				""");

		AiEvaluationResult result = this.service.evaluateWritingTask("Describe the chart", "The chart shows growth.", 1);

		assertEquals(new BigDecimal("6.5"), result.score());
		assertEquals("Bài viết trả lời đúng trọng tâm.", result.feedback().strengths().get(0));
		verify(this.geminiClient).generateJson(contains("IELTS Academic Writing Task 1"), any(Schema.class));
	}

	@Test
	void evaluateSpeakingAttempt_whenGeminiReturnsInvalidScore_shouldRejectEvaluation() {
		when(this.geminiClient.generateJson(any(String.class), any(Schema.class))).thenReturn("""
				{
				  "score": 9.5,
				  "feedback": {
				    "strengths": ["Câu trả lời rõ ràng."],
				    "weaknesses": ["Thiếu chi tiết."],
				    "improvements": ["Bổ sung ví dụ."]
				  }
				}
				""");

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> this.service.evaluateSpeakingAttempt("Question 1: Work. Candidate response: I work daily."));

		assertEquals("Gemini score is outside the valid IELTS range", exception.getMessage());
	}

	@Test
	void evaluateOverall_whenFeedbackIsMissingSection_shouldRejectFeedback() {
		when(this.geminiClient.generateJson(any(String.class), any(Schema.class))).thenReturn("""
				{
				  "strengths": ["Listening tốt."],
				  "weaknesses": [],
				  "improvements": ["Luyện đều bốn kỹ năng."]
				}
				""");

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> this.service.evaluateOverall(new BigDecimal("6.0"), new BigDecimal("6.5"),
						new BigDecimal("6.0"), new BigDecimal("6.5"), "writing", "speaking"));

		assertEquals("Gemini feedback is missing required sections", exception.getMessage());
	}

	@Test
	void evaluateWritingTask_whenMockEnabled_shouldReturnDeterministicTaskScoresWithoutCallingGemini() {
		ReflectionTestUtils.setField(this.service, "mockEnabled", true);

		AiEvaluationResult taskOne = this.service.evaluateWritingTask("Task 1", "The graph increases.", 1);
		AiEvaluationResult taskTwo = this.service.evaluateWritingTask("Task 2", "I agree with the statement.", 2);

		assertEquals(new BigDecimal("6.0"), taskOne.score());
		assertEquals(new BigDecimal("6.5"), taskTwo.score());
	}

	@Test
	void evaluateOverall_whenBandScoreIsOutsideIeltsRange_shouldRejectInputBeforeGeminiCall() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> this.service.evaluateOverall(new BigDecimal("10.0"), new BigDecimal("6.5"),
						new BigDecimal("6.0"), new BigDecimal("6.5"), null, null));

		assertEquals("IELTS band score is outside the valid range", exception.getMessage());
	}

	@Test
	void evaluateOverall_whenMockEnabled_shouldReturnDeterministicVietnameseFeedback() {
		ReflectionTestUtils.setField(this.service, "mockEnabled", true);

		AiFeedback feedback = this.service.evaluateOverall(new BigDecimal("6.0"), new BigDecimal("6.5"),
				new BigDecimal("6.0"), new BigDecimal("6.5"), null, null);

		assertEquals("Người học có nền tảng tương đối tốt ở một số kỹ năng.", feedback.strengths().get(0));
	}
}
