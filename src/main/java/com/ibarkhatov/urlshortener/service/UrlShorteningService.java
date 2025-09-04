package com.ibarkhatov.urlshortener.service;

import com.ibarkhatov.urlshortener.domain.ShortUrl;
import com.ibarkhatov.urlshortener.dto.CreateUrlRequest;
import com.ibarkhatov.urlshortener.dto.UrlResponse;
import com.ibarkhatov.urlshortener.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UrlShorteningService {
    private final ShortUrlRepository repository;

    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request) {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl(request.originalUrl());
        entity.setClickCount(0);

        ShortUrl saved = repository.save(entity);

        byte[] idBytes = ByteBuffer.allocate(Long.BYTES).putLong(saved.getId()).array();
        String shortCode = Base64.encodeBase64URLSafeString(idBytes);
        saved.setShortCode(shortCode);
        saved = repository.save(saved);

        return new UrlResponse(
                saved.getId(),
                saved.getOriginalUrl(),
                saved.getShortCode(),
                saved.getCreatedAt(),
                saved.getClickCount(),
                saved.getLastAccessedAt()
        );
    }

    @Transactional
    public Optional<String> resolveAndTrack(String shortCode) {
        return repository.findByShortCode(shortCode)
                .map(entity -> {
                    entity.setClickCount(entity.getClickCount() + 1);
                    entity.setLastAccessedAt(Instant.now());
                    repository.save(entity);
                    return entity.getOriginalUrl();
                });
    }
}
