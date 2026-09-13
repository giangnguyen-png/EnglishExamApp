package com.englishApp.exam;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.cloudinary.Cloudinary;
import com.englishApp.exam.client.GeminiClient;
import com.englishApp.exam.repository.AnswerRepository;
import com.englishApp.exam.repository.ExamRepository;
import com.englishApp.exam.repository.ExamSectionRepository;
import com.englishApp.exam.repository.MockSessionRepository;
import com.englishApp.exam.repository.PaymentRepository;
import com.englishApp.exam.repository.QuestionRepository;
import com.englishApp.exam.repository.RoleRepository;
import com.englishApp.exam.repository.SessionRegistrationRepository;
import com.englishApp.exam.repository.SkillResultRepository;
import com.englishApp.exam.repository.TestAttemptRepository;
import com.englishApp.exam.repository.UserRepository;
import com.englishApp.exam.repository.UserResponseChoiceRepository;
import com.englishApp.exam.repository.UserResponseRepository;

@SpringBootTest
@TestPropertySource(properties = {
		"jwt.secret=test-secret-for-exam-application-tests-32-bytes",
		"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
})
class ExamApplicationTests {
	@MockitoBean
	private AnswerRepository answerRepository;
	@MockitoBean
	private ExamRepository examRepository;
	@MockitoBean
	private ExamSectionRepository examSectionRepository;
	@MockitoBean
	private MockSessionRepository mockSessionRepository;
	@MockitoBean
	private PaymentRepository paymentRepository;
	@MockitoBean
	private QuestionRepository questionRepository;
	@MockitoBean
	private RoleRepository roleRepository;
	@MockitoBean
	private SessionRegistrationRepository sessionRegistrationRepository;
	@MockitoBean
	private SkillResultRepository skillResultRepository;
	@MockitoBean
	private TestAttemptRepository testAttemptRepository;
	@MockitoBean
	private UserRepository userRepository;
	@MockitoBean
	private UserResponseChoiceRepository userResponseChoiceRepository;
	@MockitoBean
	private UserResponseRepository userResponseRepository;
	@MockitoBean
	private Cloudinary cloudinary;
	@MockitoBean
	private GeminiClient geminiClient;

	@Test
	void contextLoads() {
	}

}
