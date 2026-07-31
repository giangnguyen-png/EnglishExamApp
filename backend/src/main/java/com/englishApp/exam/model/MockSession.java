package com.englishApp.exam.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.englishApp.exam.model.enums.MockSessionStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "mock_sessions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MockSession {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@NotBlank(message = "Mã phòng không được để trống")
	@Column(unique = true, name = "room_code", nullable = false)
	private String roomCode;
	@Column(name = "start_time")
	private LocalDateTime startTime;
	@Column(name = "end_time")
	private LocalDateTime endTime;
	@Column(name = "registration_deadline")
	private LocalDateTime registrationDeadline;
	@Column(name = "max_candidates")
	private int maxCandidates;
	@Enumerated(EnumType.STRING)
	@NotNull
	private MockSessionStatus status;
	@Column(name = "created_at")
	@CreationTimestamp
	private LocalDateTime createdAt;

	@ManyToOne
	@JoinColumn(name = "expert_id", nullable = false)
	@ToString.Exclude
	private User expert;
	@ManyToOne
	@JoinColumn(name = "exam_id", nullable = false)
	@ToString.Exclude
	private Exam exam;
	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<SessionRegistration> sessionRegistrations;
	@OneToMany(mappedBy = "session")
	@ToString.Exclude
	private List<TestAttempt> testAttempts;
}
