package com.englishApp.exam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.jupiter.api.Test;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.ApiExceptionFactory;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;

class SpeechServiceImplTest {
	@Test
	void shortWavUsesSingleSynchronousRecognition() {
		TestSpeechService service = new TestSpeechService(List.of(transcript("short answer")));

		String result = service.speechToText(wavSeconds(15));

		assertEquals("short answer", result);
		assertEquals(1, service.receivedChunks.size());
	}

	@Test
	void nearLimitWavStillUsesSingleRecognition() {
		TestSpeechService service = new TestSpeechService(List.of(transcript("near limit")));

		String result = service.speechToText(wavSeconds(50));

		assertEquals("near limit", result);
		assertEquals(1, service.receivedChunks.size());
	}

	@Test
	void longWavSplitsIntoValidChunksAndMergesTranscriptsInOrder() {
		TestSpeechService service = new TestSpeechService(List.of(
				transcript("first"),
				transcript("second")));

		String result = service.speechToText(wavSeconds(70));

		assertEquals("first second", result);
		assertEquals(2, service.receivedChunks.size());
		assertValidWav(service.receivedChunks.get(0));
		assertValidWav(service.receivedChunks.get(1));
	}

	@Test
	void ieltsPartTwoLengthSplitsIntoThreeChunks() {
		TestSpeechService service = new TestSpeechService(List.of(
				transcript("one"),
				transcript("two"),
				transcript("three")));

		String result = service.speechToText(wavSeconds(118));

		assertEquals("one two three", result);
		assertEquals(3, service.receivedChunks.size());
	}

	@Test
	void emptyMiddleChunkDoesNotFailWholeTranscript() {
		TestSpeechService service = new TestSpeechService(List.of(
				transcript("before"),
				transcript(""),
				transcript("after")));

		String result = service.speechToText(wavSeconds(118));

		assertEquals("before after", result);
	}

	@Test
	void transientGoogleErrorRetriesOnceThenSucceeds() {
		TestSpeechService service = new TestSpeechService(List.of(
				apiException(StatusCode.Code.UNAVAILABLE),
				transcript("retry success")));

		String result = service.speechToText(wavSeconds(15));

		assertEquals("retry success", result);
		assertEquals(2, service.receivedChunks.size());
	}

	@Test
	void permanentGoogleErrorDoesNotRetry() {
		TestSpeechService service = new TestSpeechService(List.of(apiException(StatusCode.Code.PERMISSION_DENIED)));

		assertThrows(IllegalStateException.class, () -> service.speechToText(wavSeconds(15)));
		assertEquals(1, service.receivedChunks.size());
	}

	private static RecognizeResponse transcript(String text) {
		if (text.isBlank()) {
			return RecognizeResponse.newBuilder().build();
		}
		return RecognizeResponse.newBuilder()
				.addResults(SpeechRecognitionResult.newBuilder()
						.addAlternatives(SpeechRecognitionAlternative.newBuilder().setTranscript(text)))
				.build();
	}

	private static ApiException apiException(StatusCode.Code code) {
		return ApiExceptionFactory.createException("google error", null, new TestStatusCode(code), false);
	}

	private static byte[] wavSeconds(int seconds) {
		AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
		int frameSize = format.getFrameSize();
		int frameCount = (int) format.getFrameRate() * seconds;
		byte[] audio = new byte[frameCount * frameSize];
		try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(audio), format, frameCount);
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			AudioSystem.write(stream, AudioFileFormat.Type.WAVE, output);
			return output.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void assertValidWav(byte[] audioData) {
		try (AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData))) {
			assertTrue(stream.getFrameLength() > 0);
		} catch (Exception e) {
			throw new AssertionError("Chunk is not a valid WAV", e);
		}
	}

	private static class TestSpeechService extends SpeechServiceImpl {
		private final List<Object> responses;
		private final List<byte[]> receivedChunks = new ArrayList<>();
		private int index;

		TestSpeechService(List<Object> responses) {
			super("en-US", "LINEAR16", 16000);
			this.responses = responses;
		}

		@Override
		protected RecognizeResponse recognize(RecognitionConfig config, byte[] audioData) {
			this.receivedChunks.add(audioData);
			Object response = this.responses.get(this.index++);
			if (response instanceof ApiException apiException) {
				throw apiException;
			}
			return (RecognizeResponse) response;
		}
	}

	private record TestStatusCode(StatusCode.Code code) implements StatusCode {
		@Override
		public Code getCode() {
			return code;
		}

		@Override
		public Object getTransportCode() {
			return code;
		}
	}
}
