package com.englishApp.exam.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "test_attempts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TestAttempt {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@Column(name = "overall_band_score", precision = 3, scale = 1)
	private BigDecimal overallBandScore;
	@Column(name = "ai_overall_feedback", columnDefinition = "JSON")
	private String aiOverallFeedback;
	@Column(name = "start_time")
	private LocalDateTime startTime;
	@Column(name = "end_time")
	private LocalDateTime endTime;
	@Column(name = "created_at")
	@CreationTimestamp
	private LocalDateTime createdAt;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	@ToString.Exclude
	private User user;
	@ManyToOne
	@JoinColumn(name = "exam_id", nullable = false)
	@ToString.Exclude
	private Exam exam;
	@ManyToOne
	@JoinColumn(name = "session_id")
	@ToString.Exclude
	private MockSession session;
	@OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<SkillResult> skillResults;
	@OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<UserResponse> responses;
}
