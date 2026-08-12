package com.englishApp.exam.client;

import com.google.genai.types.Schema;

public interface GeminiClient {
	String generateJson(String prompt, Schema responseSchema);
}
