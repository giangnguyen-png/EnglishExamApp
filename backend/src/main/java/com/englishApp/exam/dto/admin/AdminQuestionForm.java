package com.englishApp.exam.dto.admin;

import com.englishApp.exam.model.enums.QuestionType;

public class AdminQuestionForm {
	private QuestionType questionType;
	private String content;
	private double points = 1.0;
	private int orderIndex;
	private String imageUrl;
	private Integer durationSeconds;
	private Integer preparationSeconds;

	public QuestionType getQuestionType() {
		return questionType;
	}

	public void setQuestionType(QuestionType questionType) {
		this.questionType = questionType;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public double getPoints() {
		return points;
	}

	public void setPoints(double points) {
		this.points = points;
	}

	public int getOrderIndex() {
		return orderIndex;
	}

	public void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public Integer getDurationSeconds() {
		return durationSeconds;
	}

	public void setDurationSeconds(Integer durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

	public Integer getPreparationSeconds() {
		return preparationSeconds;
	}

	public void setPreparationSeconds(Integer preparationSeconds) {
		this.preparationSeconds = preparationSeconds;
	}
}
