package com.englishApp.exam.model;

import java.util.List;

import com.englishApp.exam.model.enums.QuestionType;

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
@Table(name = "questions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Question {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "question_type")
	private QuestionType questionType;
	@NotBlank(message = "Nội dung không được để trống!")
	@Column(columnDefinition = "TEXT")
	private String content;
	private double points;
	@Column(name = "order_index")
	private int orderIndex;
	@Column(name = "image_url", length = 500)
	private String imageUrl;
	@Column(name = "image_public_id")
	private String imagePublicId;
	@Column(name = "duration_seconds")
	private Integer durationSeconds;
	@Column(name = "preparation_seconds")
	private Integer preparationSeconds;

	@ManyToOne
	@JoinColumn(name = "exam_section_id", nullable = false)
	@ToString.Exclude
	private ExamSection examSection;
	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<Answer> answers;
	@OneToMany(mappedBy = "question")
	private List<UserResponse> responses;
}
