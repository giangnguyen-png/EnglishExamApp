package com.englishApp.exam.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
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
@Table(name = "session_registrations", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "session_id", "user_id" }),
		@UniqueConstraint(columnNames = { "session_id", "candidate_number" }) })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SessionRegistration {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private Integer candidateNumber;
	@Column(name = "registered_at")
	@CreationTimestamp
	private LocalDateTime registeredAt;

	@ManyToOne
	@JoinColumn(name = "session_id", nullable = false)
	@ToString.Exclude
	private MockSession session;
	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	@ToString.Exclude
	private User user;
}
