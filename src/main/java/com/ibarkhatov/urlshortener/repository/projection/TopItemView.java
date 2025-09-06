package com.ibarkhatov.urlshortener.repository.projection;

import java.time.Instant;

public interface TopItemView {
    String getShortCode();

    String getOriginalUrl();

    long getTotalClicks();

    Instant getLastAccessedAt();
}
