package com.englishApp.exam.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "answers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Answer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@NotBlank(message = "Nội dung không được để trống!")
	@Column(columnDefinition = "TEXT")
	private String content;
	@Column(name = "is_correct")
	private boolean isCorrect;

	@ManyToOne
	@JoinColumn(name = "question_id", nullable = false)
	@ToString.Exclude
	private Question question;
	@OneToMany(mappedBy = "answer")
	@ToString.Exclude
	private List<UserResponseChoice> userResponseChoices;
}
