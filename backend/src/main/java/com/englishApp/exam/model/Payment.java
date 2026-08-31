package com.englishApp.exam.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.englishApp.exam.model.enums.PaymentMethod;
import com.englishApp.exam.model.enums.PaymentStatus;

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
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "payments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Payment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@Column(precision = 10, scale = 2)
	private BigDecimal amount;
	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method")
	@NotNull
	private PaymentMethod paymentMethod;
	@Column(name = "transaction_id", unique = true)
	private String transactionId;
	@Enumerated(EnumType.STRING)
	@NotNull
	private PaymentStatus status;
	@Column(name = "created_at")
	@CreationTimestamp
	private LocalDateTime createdAt;
	@Column(name = "completed_at")
	private LocalDateTime completedAt;
	@Column(name = "premium_duration")
	private Integer premiumDuration;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	@ToString.Exclude
	private User user;
}
