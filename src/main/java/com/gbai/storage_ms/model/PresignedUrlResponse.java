package com.gbai.storage_ms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {
    private String url;
    private String fileId;
    private String bucket;
    private String method; // PUT or GET
    private long expiry;
} 