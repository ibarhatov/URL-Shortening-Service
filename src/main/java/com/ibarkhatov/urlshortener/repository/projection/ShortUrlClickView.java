package com.ibarkhatov.urlshortener.repository.projection;

public interface ShortUrlClickView {
    Long getId();

    String getOriginalUrl();
}
