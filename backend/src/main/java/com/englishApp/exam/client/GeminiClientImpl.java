package com.englishApp.exam.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;

@Component
public class GeminiClientImpl implements GeminiClient {
	private final String apiKey;
	private final String model;
	private Client client;

	public GeminiClientImpl(
			@Value("${gemini.api-key}") String apiKey,
			@Value("${gemini.model}") String model) {
		this.apiKey = apiKey;
		this.model = model;
	}

	public String generateJson(String prompt, Schema responseSchema) {
		if (this.apiKey == null || this.apiKey.isBlank()) {
			throw new IllegalStateException("Gemini API key is not configured");
		}

		GenerateContentConfig config = GenerateContentConfig.builder()
				.responseMimeType("application/json")
				.candidateCount(1)
				.responseSchema(responseSchema)
				.build();

		GenerateContentResponse response = client().models.generateContent(this.model, prompt, config);
		String text = response.text();
		if (text == null || text.isBlank()) {
			throw new IllegalStateException("Gemini returned an empty response");
		}
		return text;
	}

	private Client client() {
		if (this.client == null) {
			this.client = Client.builder().apiKey(this.apiKey).build();
		}
		return this.client;
	}
}
