package com.englishApp.exam.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@NotBlank(message = "Username không được để trống!")
	@Column(unique = true)
	private String username;
	@NotBlank(message = "Email không được để trống!")
	@Email(message = "Email không hợp lệ!")
	@Column(unique = true)
	private String email;
	@NotBlank(message = "Password không được để trống!")
	@Size(min = 6, message = "Password phải có ít nhất 6 ký tự!")
	private String password;
	@NotBlank(message = "Fullname không được để trống!")
	@Column(name = "full_name")
	private String fullName;
	@Column(name = "created_at")
	@CreationTimestamp
	private LocalDateTime createdAt;
	@Column(name = "updated_at")
	@UpdateTimestamp
	private LocalDateTime updatedAt;

	@ManyToOne
	@JoinColumn(name = "role_id", nullable = false)
	@ToString.Exclude
	private Role role;
	@OneToMany(mappedBy = "expert")
	@ToString.Exclude
	private List<MockSession> mockSessions;
	@OneToMany(mappedBy = "user")
	@ToString.Exclude
	private List<SessionRegistration> sessionRegistrations;
	@OneToMany(mappedBy = "user")
	@ToString.Exclude
	private List<TestAttempt> testAttempts;
	@OneToMany(mappedBy = "gradedBy")
	@ToString.Exclude
	private List<UserResponse> responses;
	@OneToMany(mappedBy = "user")
	@ToString.Exclude
	private List<Payment> payments;
}
