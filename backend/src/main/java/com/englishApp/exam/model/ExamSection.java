package com.englishApp.exam.model;

import java.util.List;

import com.englishApp.exam.model.enums.SkillType;

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
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "exam_sections")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ExamSection {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "skill_type")
	private SkillType skillType;
	@Column(name = "passage_content")
	private String passageContent;
	@Column(name = "media_url")
	private String mediaUrl;
	@Column(name = "media_public_id")
	private String mediaPublicId;
	@Column(name = "section_order")
	private int sectionOrder;

	@ManyToOne
	@JoinColumn(name = "exam_id", nullable = false)
	@ToString.Exclude
	private Exam exam;
	@OneToMany(mappedBy = "examSection", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<Question> questions;
}
