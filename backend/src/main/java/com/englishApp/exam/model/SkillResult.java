package com.englishApp.exam.model;

import java.math.BigDecimal;

import com.englishApp.exam.model.enums.SkillType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "skill_results", uniqueConstraints = @UniqueConstraint(columnNames = { "attempt_id", "skill_type" }))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SkillResult {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@Column(name = "band_score", precision = 3, scale = 1)
	private BigDecimal bandScore;
	@Column(name = "ai_analysis", columnDefinition = "JSON")
	private String aiAnalysis;
	@Enumerated(EnumType.STRING)
	@NotNull
	@Column(name = "skill_type")
	private SkillType skillType;

	@ManyToOne
	@JoinColumn(name = "attempt_id", nullable = false)
	@ToString.Exclude
	private TestAttempt attempt;
}
