package com.gbai.storage_ms.repository;

import com.gbai.storage_ms.model.VideoMetadata;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface VideoMetadataRepository extends ReactiveMongoRepository<VideoMetadata, String> {
    Flux<VideoMetadata> findByCompetitionId(String competitionId);
    Flux<VideoMetadata> findByUploaderId(String uploaderId);
} 