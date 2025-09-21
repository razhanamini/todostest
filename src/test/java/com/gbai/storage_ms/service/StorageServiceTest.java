package com.gbai.storage_ms.service;

import com.gbai.storage_ms.event.VideoEvent;
import com.gbai.storage_ms.model.PresignedUrlResponse;
import com.gbai.storage_ms.model.VideoMetadata;
import com.gbai.storage_ms.repository.VideoMetadataRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {
    @Mock
    private MinioClient minioClient;
    @Mock
    private VideoMetadataRepository metadataRepository;
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService(minioClient, metadataRepository);
        // Manually set the defaultExpiry field since @Value doesn't work in unit tests
        ReflectionTestUtils.setField(storageService, "defaultExpiry", 3600);
    }

    @Test
    void testGeneratePresignedUploadUrl() throws Exception {
        // Given
        String competitionId = "comp123";
        String uploaderId = "user123";
        String originalFilename = "video.mp4";
        String contentType = "video/mp4";
        long fileSize = 1024L;
        String presignedUrl = "http://minio/presigned-upload";
        VideoMetadata metadata = new VideoMetadata();
        metadata.setId("test-id");
        metadata.setCompetitionId(competitionId);
        metadata.setUploaderId(uploaderId);
        metadata.setOriginalFilename(originalFilename);
        metadata.setContentType(contentType);
        metadata.setFileSize(fileSize);

        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(presignedUrl);
        when(metadataRepository.save(any(VideoMetadata.class))).thenReturn(Mono.just(metadata));

        // When
        PresignedUrlResponse response = storageService.generatePresignedUploadUrl(competitionId, uploaderId, originalFilename, contentType, fileSize).block();

        // Then
        assertNotNull(response);
        assertEquals(presignedUrl, response.getUrl());
        assertEquals("PUT", response.getMethod());
        assertEquals(3600, response.getExpiry());
        verify(metadataRepository).save(any(VideoMetadata.class));
    }

    @Test
    void testGeneratePresignedDownloadUrl() throws Exception {
        // Given
        String videoId = "vid123";
        String presignedUrl = "http://minio/presigned-download";
        VideoMetadata metadata = new VideoMetadata();
        metadata.setId(videoId);
        metadata.setCompetitionId("comp123");
        metadata.setStoredFilename("stored-video.mp4");

        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(presignedUrl);
        when(metadataRepository.findById(videoId)).thenReturn(Mono.just(metadata));

        // When
        PresignedUrlResponse response = storageService.generatePresignedDownloadUrl(videoId).block();

        // Then
        assertNotNull(response);
        assertEquals(presignedUrl, response.getUrl());
        assertEquals("GET", response.getMethod());
        assertEquals(3600, response.getExpiry());
    }

    @Test
    void testDeleteVideo() throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        VideoMetadata metadata = new VideoMetadata();
        metadata.setId("vid1");
        metadata.setCompetitionId("comp1");
        metadata.setStoredFilename("vid1-video.mp4");

        when(metadataRepository.findById("vid1")).thenReturn(Mono.just(metadata));
        when(metadataRepository.deleteById("vid1")).thenReturn(Mono.empty());

        StepVerifier.create(storageService.deleteVideo("vid1"))
                .verifyComplete();

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        verify(metadataRepository).deleteById("vid1");
    }

    @Test
    void testVerifyVideoUpload_Success() throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        VideoMetadata metadata = new VideoMetadata();
        metadata.setId("vid1");
        metadata.setCompetitionId("comp1");
        metadata.setStoredFilename("vid1-video.mp4");

        when(metadataRepository.findById("vid1")).thenReturn(Mono.just(metadata));

        StepVerifier.create(storageService.verifyVideoUpload("vid1"))
                .expectNext(true)
                .verifyComplete();

        verify(minioClient).statObject(any(StatObjectArgs.class));
    }

    @Test
    void testVerifyVideoUpload_NotFound() throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        VideoMetadata metadata = new VideoMetadata();
        metadata.setId("vid1");
        metadata.setCompetitionId("comp1");
        metadata.setStoredFilename("vid1-video.mp4");

        when(metadataRepository.findById("vid1")).thenReturn(Mono.just(metadata));
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(new RuntimeException("Object not found"));

        StepVerifier.create(storageService.verifyVideoUpload("vid1"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testVerifyVideoUpload_NoMetadata() {
        when(metadataRepository.findById("vid1")).thenReturn(Mono.empty());

        StepVerifier.create(storageService.verifyVideoUpload("vid1"))
                .expectNext(false)
                .verifyComplete();
    }
} 