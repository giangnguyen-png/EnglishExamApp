package com.englishApp.exam.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.englishApp.exam.service.SpeechService;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SpeechServiceImpl implements SpeechService {
	private static final Logger logger = LoggerFactory.getLogger(SpeechServiceImpl.class);
	private static final double SHORT_AUDIO_THRESHOLD_SECONDS = 50.0;
	private static final double CHUNK_SECONDS = 50.0;

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

		AudioInfo audioInfo = readAudioInfo(audioData);
		List<AudioChunk> chunks = audioInfo.durationSeconds() <= SHORT_AUDIO_THRESHOLD_SECONDS
				? List.of(new AudioChunk(audioData, audioInfo.durationSeconds()))
				: splitWav(audioData, audioInfo);

		List<String> transcripts = new ArrayList<>();
		for (int i = 0; i < chunks.size(); i++) {
			AudioChunk chunk = chunks.get(i);
			String transcript = transcribeChunk(chunk.data(), i + 1, chunks.size(), chunk.durationSeconds());
			if (transcript != null && !transcript.isBlank()) {
				transcripts.add(transcript.trim());
			}
		}
		String finalTranscript = String.join(" ", transcripts).trim();
		if (finalTranscript.isBlank()) {
			throw new IllegalStateException("Google Speech-to-Text returned an empty transcript");
		}
		return finalTranscript;
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

	private String transcribeChunk(byte[] audioData, int chunkIndex, int totalChunks, double durationSeconds) {
		try {
			return extractTranscript(recognizeWithRetry(audioData, chunkIndex, totalChunks, durationSeconds));
		} catch (IOException e) {
			logger.error("Google STT client initialization failed for chunk {}/{} duration={}s: {}",
					chunkIndex, totalChunks, formatDuration(durationSeconds), e.getMessage(), e);
			throw new IllegalStateException("Could not initialize Google Speech-to-Text client", e);
		} catch (ApiException e) {
			logGoogleFailure(e, chunkIndex, totalChunks, durationSeconds);
			throw new IllegalStateException("Google Speech-to-Text transcription failed", e);
		} catch (RuntimeException e) {
			logger.error("Google STT failed for chunk {}/{} duration={}s: {}",
					chunkIndex, totalChunks, formatDuration(durationSeconds), e.getMessage(), e);
			throw new IllegalStateException("Google Speech-to-Text transcription failed", e);
		}
	}

	private RecognizeResponse recognizeWithRetry(byte[] audioData, int chunkIndex, int totalChunks,
			double durationSeconds) throws IOException {
		try {
			return recognize(buildRecognitionConfig(), audioData);
		} catch (ApiException e) {
			if (!isTransientGoogleError(e)) {
				throw e;
			}
			logGoogleFailure(e, chunkIndex, totalChunks, durationSeconds);
			sleepBeforeRetry();
			return recognize(buildRecognitionConfig(), audioData);
		}
	}

	protected RecognizeResponse recognize(RecognitionConfig config, byte[] audioData) throws IOException {
		RecognitionAudio audio = RecognitionAudio.newBuilder()
				.setContent(ByteString.copyFrom(audioData))
				.build();
		try (SpeechClient speechClient = SpeechClient.create()) {
			return speechClient.recognize(config, audio);
		}
	}

	private String extractTranscript(RecognizeResponse response) {
		return response.getResultsList().stream()
				.filter(result -> result.getAlternativesCount() > 0)
				.map(result -> result.getAlternatives(0).getTranscript())
				.filter(text -> text != null && !text.isBlank())
				.collect(Collectors.joining(" "))
				.trim();
	}

	private AudioInfo readAudioInfo(byte[] audioData) {
		try (AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData))) {
			AudioFormat format = stream.getFormat();
			if (format.getFrameRate() <= 0 || format.getFrameSize() <= 0 || stream.getFrameLength() < 0) {
				throw new IllegalStateException("Unsupported WAV metadata for Speech-to-Text");
			}
			double durationSeconds = stream.getFrameLength() / format.getFrameRate();
			return new AudioInfo(format, stream.getFrameLength(), durationSeconds);
		} catch (UnsupportedAudioFileException e) {
			logger.error("Speaking audio is not a supported WAV file: {}", e.getMessage(), e);
			throw new IllegalStateException("Speaking audio must be a valid WAV file", e);
		} catch (IOException e) {
			logger.error("Could not read Speaking WAV audio: {}", e.getMessage(), e);
			throw new IllegalStateException("Could not read Speaking audio", e);
		}
	}

	private List<AudioChunk> splitWav(byte[] audioData, AudioInfo audioInfo) {
		List<AudioChunk> chunks = new ArrayList<>();
		AudioFormat format = audioInfo.format();
		long framesPerChunk = Math.max(1L, Math.round(format.getFrameRate() * CHUNK_SECONDS));
		try (AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData))) {
			long remainingFrames = audioInfo.frameLength();
			while (remainingFrames > 0) {
				long framesInChunk = Math.min(framesPerChunk, remainingFrames);
				byte[] chunkPcm = readFrames(stream, framesInChunk, format.getFrameSize());
				long actualFrames = chunkPcm.length / format.getFrameSize();
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				try (AudioInputStream chunkStream = new AudioInputStream(new ByteArrayInputStream(chunkPcm), format,
						actualFrames)) {
					AudioSystem.write(chunkStream, AudioFileFormat.Type.WAVE, output);
				}
				chunks.add(new AudioChunk(output.toByteArray(), actualFrames / format.getFrameRate()));
				remainingFrames -= framesInChunk;
			}
			return chunks;
		} catch (UnsupportedAudioFileException | IOException e) {
			logger.error("Could not split Speaking WAV audio duration={}s: {}",
					formatDuration(audioInfo.durationSeconds()), e.getMessage(), e);
			throw new IllegalStateException("Could not split Speaking audio", e);
		}
	}

	private byte[] readFrames(AudioInputStream stream, long framesToRead, int frameSize) throws IOException {
		long bytesToRead = framesToRead * frameSize;
		if (bytesToRead > Integer.MAX_VALUE) {
			throw new IOException("WAV chunk is too large");
		}
		byte[] data = new byte[(int) bytesToRead];
		int offset = 0;
		while (offset < data.length) {
			int read = stream.read(data, offset, data.length - offset);
			if (read == -1) {
				break;
			}
			offset += read;
		}
		if (offset == data.length) {
			return data;
		}
		byte[] truncated = new byte[offset];
		System.arraycopy(data, 0, truncated, 0, offset);
		return truncated;
	}

	private boolean isTransientGoogleError(ApiException error) {
		StatusCode.Code code = error.getStatusCode().getCode();
		return code == StatusCode.Code.UNAVAILABLE || code == StatusCode.Code.DEADLINE_EXCEEDED;
	}

	private void sleepBeforeRetry() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void logGoogleFailure(ApiException error, int chunkIndex, int totalChunks, double durationSeconds) {
		logger.error("Google STT failed for chunk {}/{} duration={}s code={} message={}",
				chunkIndex, totalChunks, formatDuration(durationSeconds), error.getStatusCode().getCode(),
				error.getMessage(), error);
	}

	private String formatDuration(double durationSeconds) {
		return String.format("%.2f", durationSeconds);
	}

	private record AudioInfo(AudioFormat format, long frameLength, double durationSeconds) {
	}

	private record AudioChunk(byte[] data, double durationSeconds) {
	}
}
