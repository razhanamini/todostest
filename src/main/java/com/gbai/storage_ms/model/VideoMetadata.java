package com.gbai.storage_ms.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "videos")
public class VideoMetadata {
    @Id
    private String id;
    private String competitionId;
    private String uploaderId;
    private String originalFilename;
    private String storedFilename;
    private Instant uploadTimestamp;
    private long fileSize;
    private String contentType;
} 