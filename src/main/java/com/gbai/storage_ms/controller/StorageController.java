package com.gbai.storage_ms.controller;

import com.gbai.storage_ms.model.ApiResponse;
import com.gbai.storage_ms.model.PresignedUrlResponse;
import com.gbai.storage_ms.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.gbai.storage_ms.model.VideoMetadata;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "Endpoints for video storage and retrieval")
public class StorageController {
    private final StorageService storageService;

    @PostMapping("/presigned-upload")
    @Operation(summary = "Get presigned upload URL for a competition video")
    public Mono<ApiResponse<PresignedUrlResponse>> getPresignedUploadUrl(@RequestParam String competitionId,
                                                                         @RequestParam String uploaderId,
                                                                         @RequestParam String originalFilename,
                                                                         @RequestParam String contentType,
                                                                         @RequestParam long fileSize) {
        return storageService.generatePresignedUploadUrl(competitionId, uploaderId, originalFilename, contentType, fileSize)
                .map(url -> new ApiResponse<>(200, "Presigned upload URL generated", url));
    }

    @GetMapping("/presigned-download/{videoId}")
    @Operation(summary = "Get presigned download URL for a video")
    public Mono<ApiResponse<PresignedUrlResponse>> getPresignedDownloadUrl(@PathVariable String videoId) {
        return storageService.generatePresignedDownloadUrl(videoId)
                .map(url -> new ApiResponse<>(200, "Presigned download URL generated", url));
    }

    @GetMapping("/competition/{competitionId}/videos")
    @Operation(summary = "List presigned download URLs for all videos in a competition")
    public Flux<ApiResponse<PresignedUrlResponse>> listVideosForCompetition(@PathVariable String competitionId) {
        return storageService.listPresignedDownloadUrlsForCompetition(competitionId)
                .map(url -> new ApiResponse<>(200, "Presigned download URL", url));
    }

    @Operation(summary = "Verify video upload completion", description = "Check if a video was successfully uploaded to MinIO")
    @GetMapping("/verify/{videoId}")
    public Mono<ApiResponse<Boolean>> verifyVideoUpload(@PathVariable String videoId) {
        return storageService.verifyVideoUpload(videoId)
                .map(verified -> new ApiResponse<>(200, "Video upload verification completed", verified))
                .onErrorReturn(new ApiResponse<>(500, "Error verifying video upload", false));
    }

    @DeleteMapping("/{videoId}")
    @Operation(summary = "Delete a video and its metadata")
    public Mono<ApiResponse<Void>> deleteVideo(@PathVariable String videoId) {
        return storageService.deleteVideo(videoId)
                .thenReturn(new ApiResponse<>(200, "Video deleted", null));
    }

    @Operation(summary = "Get video metadata by ID")
    @GetMapping("/videos/{videoId}")
    public Mono<ResponseEntity<ApiResponse<VideoMetadata>>> getVideoMetadata(
            @Parameter(description = "Video ID") @PathVariable String videoId) {
        return storageService.getVideoMetadata(videoId)
                .map(metadata -> ResponseEntity.ok(ApiResponse.<VideoMetadata>builder()
                        .status(200)
                        .message("Video metadata retrieved successfully")
                        .data(metadata)
                        .build()))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Delete user video data (cascade deletion)")
    @DeleteMapping("/user/{userId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteUserVideos(@Parameter(description = "User ID") @PathVariable String userId) {
        return storageService.deleteUserVideos(userId)
                .thenReturn(ResponseEntity.ok(ApiResponse.<Void>builder()
                    .status(200)
                    .message("User video data deleted successfully")
                    .data(null)
                    .build()));
    }
} 