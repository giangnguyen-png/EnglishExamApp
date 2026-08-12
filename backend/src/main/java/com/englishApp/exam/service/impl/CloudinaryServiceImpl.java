package com.englishApp.exam.service.impl;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.englishApp.exam.dto.cloudinary.CloudinaryUploadResult;
import com.englishApp.exam.model.enums.CloudinaryResourceType;
import com.englishApp.exam.service.CloudinaryService;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {
	private final Cloudinary cloudinary;

	public CloudinaryServiceImpl(Cloudinary cloudinary) {
		this.cloudinary = cloudinary;
	}

	public CloudinaryUploadResult upload(MultipartFile file, CloudinaryResourceType resourceType) {
		validateFile(file);

		try {
			Map<?, ?> uploadResult = this.cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
					"resource_type", resourceType.value(),
					"folder", resourceType.folder(),
					"use_filename", true,
					"unique_filename", true));

			String secureUrl = (String) uploadResult.get("secure_url");
			String publicId = (String) uploadResult.get("public_id");
			if (secureUrl == null || publicId == null) {
				throw new IllegalStateException("Cloudinary upload response is missing secure_url or public_id");
			}

			return new CloudinaryUploadResult(secureUrl, publicId);
		} catch (IOException e) {
			throw new IllegalStateException("Could not upload file to Cloudinary", e);
		}
	}

	public CloudinaryUploadResult uploadImage(MultipartFile file) {
		return upload(file, CloudinaryResourceType.IMAGE);
	}

	public CloudinaryUploadResult uploadAudio(MultipartFile file) {
		return upload(file, CloudinaryResourceType.AUDIO);
	}

	public CloudinaryUploadResult uploadRawFile(MultipartFile file) {
		return upload(file, CloudinaryResourceType.RAW);
	}

	public void delete(String publicId) {
		delete(publicId, CloudinaryResourceType.IMAGE);
	}

	public void delete(String publicId, CloudinaryResourceType resourceType) {
		if (publicId == null || publicId.isBlank()) {
			throw new IllegalArgumentException("Cloudinary publicId is required");
		}

		try {
			this.cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType.value()));
		} catch (IOException e) {
			throw new IllegalStateException("Could not delete file from Cloudinary", e);
		}
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File is required");
		}
	}
}
