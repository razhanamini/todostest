package com.gbai.storage_ms.service;

import com.gbai.storage_ms.model.PresignedUrlResponse;
import com.gbai.storage_ms.model.VideoMetadata;
import com.gbai.storage_ms.repository.VideoMetadataRepository;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
    private final MinioClient minioClient;
    private final VideoMetadataRepository metadataRepository;

    @Value("${minio.default-expiry:3600}")
    private int defaultExpiry;

    public Mono<String> getOrCreateBucket(String competitionId) {
        String bucket = "competition-" + competitionId;
        return Mono.fromCallable(() -> {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            return bucket;
        });
    }

    public Mono<PresignedUrlResponse> generatePresignedUploadUrl(String competitionId, String uploaderId, String originalFilename, String contentType, long fileSize) {
        String fileId = UUID.randomUUID().toString();
        String storedFilename = fileId + "-" + originalFilename;
        return getOrCreateBucket(competitionId)
            .flatMap(bucket -> Mono.fromCallable(() -> {
                String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucket)
                        .object(storedFilename)
                        .expiry(defaultExpiry)
                        .build()
                );
                VideoMetadata metadata = new VideoMetadata();
                metadata.setId(fileId);
                metadata.setCompetitionId(competitionId);
                metadata.setUploaderId(uploaderId);
                metadata.setOriginalFilename(originalFilename);
                metadata.setStoredFilename(storedFilename);
                metadata.setUploadTimestamp(Instant.now());
                metadata.setFileSize(fileSize);
                metadata.setContentType(contentType);
                metadataRepository.save(metadata).subscribe();
                return new PresignedUrlResponse(url, fileId, bucket, "PUT", defaultExpiry);
            }));
    }

    public Mono<PresignedUrlResponse> generatePresignedDownloadUrl(String videoId) {
        return metadataRepository.findById(videoId)
            .flatMap(metadata -> Mono.fromCallable(() -> {
                String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket("competition-" + metadata.getCompetitionId())
                        .object(metadata.getStoredFilename())
                        .expiry(defaultExpiry)
                        .build()
                );
                return new PresignedUrlResponse(url, videoId, "competition-" + metadata.getCompetitionId(), "GET", defaultExpiry);
            }));
    }

    public Flux<PresignedUrlResponse> listPresignedDownloadUrlsForCompetition(String competitionId) {
        return metadataRepository.findByCompetitionId(competitionId)
            .flatMap(metadata -> generatePresignedDownloadUrl(metadata.getId()));
    }

    public Mono<Boolean> verifyVideoUpload(String videoId) {
        return metadataRepository.findById(videoId)
            .flatMap(metadata -> Mono.fromCallable(() -> {
                try {
                    // Check if object exists in MinIO
                    minioClient.statObject(StatObjectArgs.builder()
                        .bucket("competition-" + metadata.getCompetitionId())
                        .object(metadata.getStoredFilename())
                        .build());
                    return true; // Object exists
                } catch (Exception e) {
                    log.warn("Video {} not found in MinIO: {}", videoId, e.getMessage());
                    return false; // Object doesn't exist
                }
            }))
            .defaultIfEmpty(false); // No metadata found
    }

    public Mono<Void> deleteVideo(String videoId) {
        return metadataRepository.findById(videoId)
            .flatMap(metadata -> Mono.fromRunnable(() -> {
                try {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket("competition-" + metadata.getCompetitionId())
                        .object(metadata.getStoredFilename())
                        .build());
                    metadataRepository.deleteById(videoId).subscribe();
                } catch (Exception e) {
                    log.error("Failed to delete video from MinIO", e);
                }
            }));
    }

    public Mono<VideoMetadata> getVideoMetadata(String videoId) {
        return metadataRepository.findById(videoId);
    }

    public Mono<Void> deleteUserVideos(String userId) {
        log.info("Deleting video data for user: {}", userId);
        return metadataRepository.findByUploaderId(userId)
                .collectList()
                .flatMap(videos -> {
                    log.info("Found {} videos to delete for user: {}", videos.size(), userId);
                    return Flux.fromIterable(videos)
                            .flatMap(video -> deleteVideo(video.getId()))
                            .then();
                })
                .doOnSuccess(v -> log.info("Successfully deleted video data for user: {}", userId))
                .doOnError(error -> log.error("Failed to delete video data for user: {}", userId, error));
    }
} 