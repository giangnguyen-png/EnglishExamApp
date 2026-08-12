package com.englishApp.exam.service.impl;

import java.io.IOException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.englishApp.exam.service.SpeechService;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.protobuf.ByteString;

@Service
public class SpeechServiceImpl implements SpeechService {
	private final String languageCode;
	private final String audioEncoding;
	private final int sampleRateHertz;

	public SpeechServiceImpl(
			@Value("${google.speech.language-code}") String languageCode,
			@Value("${google.speech.audio-encoding}") String audioEncoding,
			@Value("${google.speech.sample-rate-hertz}") int sampleRateHertz) {
		this.languageCode = languageCode;
		this.audioEncoding = audioEncoding;
		this.sampleRateHertz = sampleRateHertz;
	}

	public String speechToText(byte[] audioData) {
		if (audioData == null || audioData.length == 0) {
			throw new IllegalArgumentException("Audio data is required");
		}

		RecognitionConfig config = buildRecognitionConfig();
		RecognitionAudio audio = RecognitionAudio.newBuilder()
				.setContent(ByteString.copyFrom(audioData))
				.build();

		try (SpeechClient speechClient = SpeechClient.create()) {
			RecognizeResponse response = speechClient.recognize(config, audio);
			String transcript = response.getResultsList().stream()
					.filter(result -> result.getAlternativesCount() > 0)
					.map(result -> result.getAlternatives(0).getTranscript())
					.filter(text -> text != null && !text.isBlank())
					.collect(Collectors.joining(" "))
					.trim();
			if (transcript.isBlank()) {
				throw new IllegalStateException("Google Speech-to-Text returned an empty transcript");
			}
			return transcript;
		} catch (IOException e) {
			throw new IllegalStateException("Could not initialize Google Speech-to-Text client", e);
		} catch (RuntimeException e) {
			throw new IllegalStateException("Google Speech-to-Text transcription failed", e);
		}
	}

	private RecognitionConfig buildRecognitionConfig() {
		RecognitionConfig.Builder builder = RecognitionConfig.newBuilder()
				.setLanguageCode(this.languageCode)
				.setEnableAutomaticPunctuation(true)
				.setMaxAlternatives(1);

		if (this.audioEncoding != null && !this.audioEncoding.isBlank()
				&& !"ENCODING_UNSPECIFIED".equalsIgnoreCase(this.audioEncoding)) {
			builder.setEncoding(RecognitionConfig.AudioEncoding.valueOf(this.audioEncoding));
		}
		if (this.sampleRateHertz > 0) {
			builder.setSampleRateHertz(this.sampleRateHertz);
		}
		return builder.build();
	}
}
