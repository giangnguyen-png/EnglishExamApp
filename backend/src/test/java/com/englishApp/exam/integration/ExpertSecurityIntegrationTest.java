package com.englishApp.exam.integration;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cloudinary.Cloudinary;
import com.englishApp.exam.client.GeminiClient;
import com.englishApp.exam.model.User;
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
import com.englishApp.exam.service.ExamService;
import com.englishApp.exam.service.MockSessionService;
import com.englishApp.exam.service.SessionRegistrationService;
import com.englishApp.exam.service.TestAttemptService;
import com.englishApp.exam.service.UserResponseService;
import com.englishApp.exam.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"jwt.secret=test-secret-for-security-integration-tests-32-bytes",
		"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
})
class ExpertSecurityIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MockSessionService mockSessionService;
	@MockitoBean
	private SessionRegistrationService sessionRegistrationService;
	@MockitoBean
	private TestAttemptService testAttemptService;
	@MockitoBean
	private UserResponseService userResponseService;
	@MockitoBean
	private UserService userService;
	@MockitoBean
	private ExamService examService;
	@MockitoBean
	private QuestionRepository questionRepository;
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
	void expertEndpoint_whenUserRoleCalls_shouldReturnForbidden() throws Exception {
		this.mockMvc.perform(get("/api/expert/mock-sessions")
						.with(jwt().jwt(jwt -> jwt.subject("learner").claim("roles", List.of("USER")))
								.authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void expertEndpoint_whenExpertRoleCalls_shouldReachController() throws Exception {
		User expert = new User();
		expert.setId(9);
		expert.setUsername("expert");
		when(this.userService.findByUsername("expert")).thenReturn(expert);
		when(this.mockSessionService.findByExpert(9)).thenReturn(List.of());

		this.mockMvc.perform(get("/api/expert/mock-sessions")
						.with(jwt().jwt(jwt -> jwt.subject("expert").claim("roles", List.of("EXPERT")))
								.authorities(new SimpleGrantedAuthority("ROLE_EXPERT"))))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}
}
