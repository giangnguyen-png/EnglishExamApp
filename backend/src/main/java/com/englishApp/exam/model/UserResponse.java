package com.englishApp.exam.model;

import java.math.BigDecimal;
import java.util.List;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "user_responses", uniqueConstraints = @UniqueConstraint(columnNames = { "attempt_id", "question_id" }))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserResponse {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@Column(name = "text_content", columnDefinition = "TEXT")
	private String textContent;
	private String fileUrl;
	private String filePublicId;
	@Column(name = "speech_to_text_trans", columnDefinition = "TEXT")
	private String speechToTextTrans;
	@Column(name = "ai_score", precision = 3, scale = 1)
	private BigDecimal aiScore;
	@Column(name = "expert_score", precision = 3, scale = 1)
	private BigDecimal expertScore;

	@ManyToOne
	@JoinColumn(name = "attempt_id", nullable = false)
	@ToString.Exclude
	private TestAttempt attempt;
	@ManyToOne
	@JoinColumn(name = "question_id", nullable = false)
	@ToString.Exclude
	private Question question;
	@ManyToOne
	@JoinColumn(name = "graded_by")
	@ToString.Exclude
	private User gradedBy;
	@OneToMany(mappedBy = "response", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<UserResponseChoice> answers;
}
