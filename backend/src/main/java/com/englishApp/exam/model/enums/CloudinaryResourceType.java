package com.englishApp.exam.model.enums;

public enum CloudinaryResourceType {
	IMAGE("image", "english-app/images"),
	AUDIO("video", "english-app/audio"),
	RAW("raw", "english-app/files");

	private final String value;
	private final String folder;

	CloudinaryResourceType(String value, String folder) {
		this.value = value;
		this.folder = folder;
	}

	public String value() {
		return this.value;
	}

	public String folder() {
		return this.folder;
	}
}
