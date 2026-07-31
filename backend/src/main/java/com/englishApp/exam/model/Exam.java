package com.englishApp.exam.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "exams")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Exam {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@NotBlank(message = "Title không được để trống!")
	@Column(unique = true)
	private String title;
	private String description;
	@Column(name = "is_premium_only")
	private boolean isPremiumOnly;
	@Column(name = "created_at")
	@CreationTimestamp
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<ExamSection> examSections;
	@OneToMany(mappedBy = "exam")
	@ToString.Exclude
	private List<MockSession> mockSessions;
	@OneToMany(mappedBy = "exam")
	@ToString.Exclude
	private List<TestAttempt> testAttempts;
}
