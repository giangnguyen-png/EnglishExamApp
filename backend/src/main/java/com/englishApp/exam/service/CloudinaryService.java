package com.englishApp.exam.service;

import org.springframework.web.multipart.MultipartFile;

import com.englishApp.exam.dto.cloudinary.CloudinaryUploadResult;
import com.englishApp.exam.model.enums.CloudinaryResourceType;

public interface CloudinaryService {
	CloudinaryUploadResult upload(MultipartFile file, CloudinaryResourceType resourceType);

	CloudinaryUploadResult uploadImage(MultipartFile file);

	CloudinaryUploadResult uploadAudio(MultipartFile file);

	CloudinaryUploadResult uploadRawFile(MultipartFile file);

	void delete(String publicId);

	void delete(String publicId, CloudinaryResourceType resourceType);
}
