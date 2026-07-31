package com.englishApp.exam.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "user_response_choices", uniqueConstraints = @UniqueConstraint(columnNames = { "response_id",
		"answer_id" }))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserResponseChoice {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "response_id", nullable = false)
	@ToString.Exclude
	private UserResponse response;

	@ManyToOne
	@JoinColumn(name = "answer_id", nullable = false)
	@ToString.Exclude
	private Answer answer;
}
