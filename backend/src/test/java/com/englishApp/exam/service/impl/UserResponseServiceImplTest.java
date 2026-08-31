package com.englishApp.exam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.englishApp.exam.dto.cloudinary.CloudinaryUploadResult;
import com.englishApp.exam.model.Exam;
import com.englishApp.exam.model.ExamSection;
import com.englishApp.exam.model.MockSession;
import com.englishApp.exam.model.Question;
import com.englishApp.exam.model.TestAttempt;
import com.englishApp.exam.model.UserResponse;
import com.englishApp.exam.model.enums.MockSessionStatus;
import com.englishApp.exam.model.enums.SkillType;
import com.englishApp.exam.repository.AnswerRepository;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.QuestionRepository;
import com.englishApp.exam.repository.SkillResultRepository;
import com.englishApp.exam.repository.TestAttemptRepository;
import com.englishApp.exam.repository.UserResponseChoiceRepository;
import com.englishApp.exam.repository.UserResponseRepository;
import com.englishApp.exam.service.AiService;
import com.englishApp.exam.service.CloudinaryService;
import com.englishApp.exam.service.SpeechService;

@ExtendWith(MockitoExtension.class)
class UserResponseServiceImplTest {
	@Mock
	private UserResponseRepository userResponseRepository;
	@Mock
	private UserResponseChoiceRepository userResponseChoiceRepository;
	@Mock
	private TestAttemptRepository testAttemptRepository;
	@Mock
	private QuestionRepository questionRepository;
	@Mock
	private AnswerRepository answerRepository;
	@Mock
	private SkillResultRepository skillResultRepository;
	@Mock
	private MockSessionRepository mockSessionRepository;
	@Mock
	private AiService aiService;
	@Mock
	private SpeechService speechService;
	@Mock
	private CloudinaryService cloudinaryService;

	private UserResponseServiceImpl service;

	@BeforeEach
	void setUp() {
		this.service = new UserResponseServiceImpl(this.userResponseRepository, this.userResponseChoiceRepository,
				this.testAttemptRepository, this.questionRepository, this.answerRepository,
				this.skillResultRepository, this.mockSessionRepository, this.aiService, this.speechService,
				this.cloudinaryService);
	}

	@Test
	void premiumSpeakingKeepsAudioWhenSpeechToTextFails() throws Exception {
		TestAttempt attempt = attempt(true);
		Question question = speakingQuestion();
		MockMultipartFile audio = audioFile();
		when(this.testAttemptRepository.findByIdForUpdate(1)).thenReturn(Optional.of(attempt));
		when(this.questionRepository.findById(2)).thenReturn(Optional.of(question));
		when(this.userResponseRepository.findByAttemptIdAndQuestionId(1, 2)).thenReturn(Optional.empty());
		when(this.cloudinaryService.uploadAudio(audio)).thenReturn(new CloudinaryUploadResult("https://audio", "public-id"));
		when(this.speechService.speechToText(any(byte[].class))).thenThrow(new IllegalStateException("STT failed"));
		when(this.userResponseRepository.save(any(UserResponse.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = this.service.submitSpeaking(1, 2, audio);

		assertEquals("https://audio", response.getFileUrl());
		assertEquals("public-id", response.getFilePublicId());
		assertNull(response.getSpeechToTextTrans());
	}

	@Test
	void normalSpeakingStillFailsWhenSpeechToTextFails() throws Exception {
		TestAttempt attempt = attempt(false);
		Question question = speakingQuestion();
		MockMultipartFile audio = audioFile();
		when(this.testAttemptRepository.findByIdForUpdate(1)).thenReturn(Optional.of(attempt));
		when(this.questionRepository.findById(2)).thenReturn(Optional.of(question));
		when(this.userResponseRepository.findByAttemptIdAndQuestionId(1, 2)).thenReturn(Optional.empty());
		when(this.cloudinaryService.uploadAudio(audio)).thenReturn(new CloudinaryUploadResult("https://audio", "public-id"));
		when(this.speechService.speechToText(any(byte[].class))).thenThrow(new IllegalStateException("STT failed"));

		assertThrows(IllegalStateException.class, () -> this.service.submitSpeaking(1, 2, audio));
		verify(this.userResponseRepository, never()).save(any(UserResponse.class));
	}

	@Test
	void normalSpeakingSavesTranscriptWhenSpeechToTextSucceeds() {
		TestAttempt attempt = attempt(false);
		Question question = speakingQuestion();
		MockMultipartFile audio = audioFile();
		when(this.testAttemptRepository.findByIdForUpdate(1)).thenReturn(Optional.of(attempt));
		when(this.questionRepository.findById(2)).thenReturn(Optional.of(question));
		when(this.userResponseRepository.findByAttemptIdAndQuestionId(1, 2)).thenReturn(Optional.empty());
		when(this.cloudinaryService.uploadAudio(audio)).thenReturn(new CloudinaryUploadResult("https://audio", "public-id"));
		when(this.speechService.speechToText(any(byte[].class))).thenReturn("spoken answer");
		when(this.userResponseRepository.save(any(UserResponse.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = this.service.submitSpeaking(1, 2, audio);

		assertEquals("https://audio", response.getFileUrl());
		assertEquals("public-id", response.getFilePublicId());
		assertEquals("spoken answer", response.getSpeechToTextTrans());
	}

	private TestAttempt attempt(boolean premium) {
		Exam exam = new Exam();
		exam.setId(10);
		TestAttempt attempt = new TestAttempt();
		attempt.setExam(exam);
		if (premium) {
			MockSession session = new MockSession();
			session.setStatus(MockSessionStatus.ONGOING);
			attempt.setSession(session);
		}
		return attempt;
	}

	private Question speakingQuestion() {
		Exam exam = new Exam();
		exam.setId(10);
		ExamSection section = new ExamSection();
		section.setExam(exam);
		section.setSkillType(SkillType.SPEAKING);
		Question question = new Question();
		question.setId(2);
		question.setExamSection(section);
		return question;
	}

	private MockMultipartFile audioFile() {
		return new MockMultipartFile("audioFile", "speaking.wav", "audio/wav", new byte[] { 1, 2, 3 });
	}
}
