package com.englishApp.exam.dto.admin;

import com.englishApp.exam.model.enums.SkillType;

public class AdminSectionForm {
	private SkillType skillType;
	private int sectionOrder;
	private String passageContent;
	private String mediaUrl;

	public SkillType getSkillType() {
		return skillType;
	}

	public void setSkillType(SkillType skillType) {
		this.skillType = skillType;
	}

	public int getSectionOrder() {
		return sectionOrder;
	}

	public void setSectionOrder(int sectionOrder) {
		this.sectionOrder = sectionOrder;
	}

	public String getPassageContent() {
		return passageContent;
	}

	public void setPassageContent(String passageContent) {
		this.passageContent = passageContent;
	}

	public String getMediaUrl() {
		return mediaUrl;
	}

	public void setMediaUrl(String mediaUrl) {
		this.mediaUrl = mediaUrl;
	}
}
