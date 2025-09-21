package com.gbai.storage_ms.controller;

import com.gbai.storage_ms.model.ApiResponse;
import com.gbai.storage_ms.model.PresignedUrlResponse;
import com.gbai.storage_ms.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(StorageController.class)
class StorageControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private StorageService storageService;

    private PresignedUrlResponse presignedUrlResponse;

    @BeforeEach
    void setUp() {
        presignedUrlResponse = new PresignedUrlResponse("http://minio/presigned", "vid1", "competition-comp1", "PUT", 600);
    }

    @Test
    void testGetPresignedUploadUrl() {
        when(storageService.generatePresignedUploadUrl(any(), any(), any(), any(), any(Long.class)))
                .thenReturn(Mono.just(presignedUrlResponse));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/storage/presigned-upload")
                        .queryParam("competitionId", "comp1")
                        .queryParam("uploaderId", "user1")
                        .queryParam("originalFilename", "video.mp4")
                        .queryParam("contentType", "video/mp4")
                        .queryParam("fileSize", 12345)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApiResponse.class)
                .value(resp -> {
                    ApiResponse<?> apiResp = (ApiResponse<?>) resp;
                    assert apiResp.getStatus() == 200;
                });
    }

    @Test
    void testGetPresignedDownloadUrl() {
        when(storageService.generatePresignedDownloadUrl(eq("vid1")))
                .thenReturn(Mono.just(presignedUrlResponse));

        webTestClient.get()
                .uri("/storage/presigned-download/vid1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApiResponse.class)
                .value(resp -> {
                    ApiResponse<?> apiResp = (ApiResponse<?>) resp;
                    assert apiResp.getStatus() == 200;
                });
    }

    @Test
    void testListVideosForCompetition() {
        when(storageService.listPresignedDownloadUrlsForCompetition(eq("comp1")))
                .thenReturn(Flux.just(presignedUrlResponse));

        webTestClient.get()
                .uri("/storage/competition/comp1/videos")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ApiResponse.class)
                .value(list -> {
                    assert !list.isEmpty();
                    assert ((ApiResponse<?>) list.get(0)).getStatus() == 200;
                });
    }

    @Test
    void testDeleteVideo() {
        when(storageService.deleteVideo(eq("vid1"))).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/storage/vid1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApiResponse.class)
                .value(resp -> {
                    ApiResponse<?> apiResp = (ApiResponse<?>) resp;
                    assert apiResp.getStatus() == 200;
                });
    }

    @Test
    void testVerifyVideoUpload() {
        when(storageService.verifyVideoUpload(eq("vid1")))
                .thenReturn(Mono.just(true));

        webTestClient.get()
                .uri("/storage/verify/vid1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApiResponse.class)
                .value(resp -> {
                    ApiResponse<?> apiResp = (ApiResponse<?>) resp;
                    assert apiResp.getStatus() == 200;
                    assert (Boolean) apiResp.getData() == true;
                });
    }
} 