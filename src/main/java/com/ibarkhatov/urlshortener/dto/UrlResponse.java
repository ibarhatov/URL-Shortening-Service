package com.ibarkhatov.urlshortener.dto;

import java.time.Instant;

public record UrlResponse(
        Long id,
        String originalUrl,
        String shortCode,
        Instant createdAt,
        long clickCount,
        Instant lastAccessedAt
) {
}


