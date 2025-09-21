package com.gbai.storage_ms.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoEvent {
    private String eventType; // videoUploaded, videoDeleted
    private String videoId;
    private String competitionId;
    private String uploaderId;
    private String filename;
    private long timestamp;
} 