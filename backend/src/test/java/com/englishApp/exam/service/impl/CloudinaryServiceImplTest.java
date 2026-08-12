package com.englishApp.exam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.englishApp.exam.dto.cloudinary.CloudinaryUploadResult;
import com.englishApp.exam.model.enums.CloudinaryResourceType;

class CloudinaryServiceImplTest {
	private final Cloudinary cloudinary = org.mockito.Mockito.mock(Cloudinary.class);
	private final Uploader uploader = org.mockito.Mockito.mock(Uploader.class);
	private final CloudinaryServiceImpl service = new CloudinaryServiceImpl(this.cloudinary);

	@Test
	void uploadAudioReturnsSecureUrlAndPublicId() throws IOException {
		MockMultipartFile file = new MockMultipartFile("file", "answer.mp3", "audio/mpeg", "audio".getBytes());
		when(this.cloudinary.uploader()).thenReturn(this.uploader);
		when(this.uploader.upload(any(byte[].class), any(Map.class))).thenReturn(Map.of(
				"secure_url", "https://res.cloudinary.com/demo/video/upload/answer.mp3",
				"public_id", "english-app/audio/answer"));

		CloudinaryUploadResult result = this.service.uploadAudio(file);

		assertEquals("https://res.cloudinary.com/demo/video/upload/answer.mp3", result.secureUrl());
		assertEquals("english-app/audio/answer", result.publicId());
	}

	@Test
	void uploadRejectsEmptyFile() {
		MockMultipartFile file = new MockMultipartFile("file", new byte[0]);

		assertThrows(IllegalArgumentException.class, () -> this.service.uploadImage(file));
	}

	@Test
	void deleteUsesRequestedResourceType() throws IOException {
		when(this.cloudinary.uploader()).thenReturn(this.uploader);

		this.service.delete("english-app/audio/answer", CloudinaryResourceType.AUDIO);

		verify(this.uploader).destroy(eq("english-app/audio/answer"), any(Map.class));
	}

	@Test
	void deleteWrapsCloudinaryFailure() throws IOException {
		when(this.cloudinary.uploader()).thenReturn(this.uploader);
		when(this.uploader.destroy(eq("english-app/files/file"), any(Map.class))).thenThrow(new IOException("failed"));

		assertThrows(IllegalStateException.class,
				() -> this.service.delete("english-app/files/file", CloudinaryResourceType.RAW));
	}
}
