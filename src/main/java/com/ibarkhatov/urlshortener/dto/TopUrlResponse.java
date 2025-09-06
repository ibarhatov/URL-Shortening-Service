package com.ibarkhatov.urlshortener.dto;

import java.time.Instant;

public record TopUrlResponse(
        String shortCode,
        String originalUrl,
        long clickCount,
        Instant lastAccessedAt
) {
}
