package com.englishApp.exam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Configuration
public class CloudinaryConfig {
	private final String cloudName;
	private final String apiKey;
	private final String apiSecret;

	public CloudinaryConfig(
			@Value("${cloudinary.cloud-name}") String cloudName,
			@Value("${cloudinary.api-key}") String apiKey,
			@Value("${cloudinary.api-secret}") String apiSecret) {
		this.cloudName = cloudName;
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}

	@Bean
	Cloudinary cloudinary() {
		return new Cloudinary(ObjectUtils.asMap(
				"cloud_name", this.cloudName,
				"api_key", this.apiKey,
				"api_secret", this.apiSecret,
				"secure", true));
	}
}
