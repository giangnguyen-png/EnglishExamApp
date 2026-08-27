package com.englishApp.exam.dto.admin;

public class AdminExamForm {
	private String title;
	private String description;
	private boolean premiumOnly;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isPremiumOnly() {
		return premiumOnly;
	}

	public void setPremiumOnly(boolean premiumOnly) {
		this.premiumOnly = premiumOnly;
	}
}
