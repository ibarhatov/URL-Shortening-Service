package com.ibarkhatov.urlshortener.service;

import com.ibarkhatov.urlshortener.domain.ShortUrl;
import com.ibarkhatov.urlshortener.dto.CreateUrlRequest;
import com.ibarkhatov.urlshortener.dto.TopUrlResponse;
import com.ibarkhatov.urlshortener.dto.UrlResponse;
import com.ibarkhatov.urlshortener.mapper.ShortUrlMapper;
import com.ibarkhatov.urlshortener.mapper.TopUrlMapper;
import com.ibarkhatov.urlshortener.repository.ShortUrlRepository;
import com.ibarkhatov.urlshortener.repository.projection.TopItemView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UrlShorteningService {
    private final ShortUrlRepository repository;
    private final ShortUrlMapper mapper;
    private final TopUrlMapper topUrlMapper;

    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request) {
        log.info("Create url");
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl(request.originalUrl());
        entity.setClickCount(0);

        ShortUrl saved = repository.save(entity);

        byte[] idBytes = ByteBuffer.allocate(Long.BYTES).putLong(saved.getId()).array();
        String shortCode = Base64.encodeBase64URLSafeString(idBytes);
        saved.setShortCode(shortCode);
        saved = repository.save(saved);
        log.info("Created id={} code={}", saved.getId(), saved.getShortCode());

        return mapper.toDto(saved);
    }

    @Transactional
    public Optional<String> resolveAndTrack(String shortCode) {
        log.info("Resolve code={}", shortCode);
        return repository.incrementAndReturn(shortCode)
                .map(shortUrl -> {
                    log.info("Resolved id={} code={}", shortUrl.getId(), shortCode);
                    return shortUrl.getOriginalUrl();
                });
    }

    @Transactional(readOnly = true)
    public List<UrlResponse> listAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public boolean deleteById(Long id) {
        log.info("Delete id={}", id);
        if (!repository.existsById(id)) {
            log.info("Not found id={}", id);
            return false;
        }
        repository.deleteById(id);
        log.info("Deleted id={}", id);
        return true;
    }

    @Transactional(readOnly = true)
    public List<TopUrlResponse> getTopByWindow(String window, int limit) {
        log.info("Get top by window: window={}, limit={}", window, limit);
        List<TopItemView> views = Strings.isBlank(window)
                ? repository.findTopByClickCount(limit)
                : repository.findTopByWindow(Instant.now().minus(parseWindow(window)).atOffset(ZoneOffset.UTC), limit);
        log.info("Get top by window: items={}", views.size());
        return topUrlMapper.toDtoList(views);
    }

    private Duration parseWindow(String window) {
        String v = window.trim().toLowerCase();
        try {
            long value = Long.parseLong(v.substring(0, v.length() - 1).trim());
            if (v.endsWith("d")) return Duration.ofDays(value);
            if (v.endsWith("h")) return Duration.ofHours(value);
            if (v.endsWith("m")) return Duration.ofMinutes(value);
            if (v.endsWith("s")) return Duration.ofSeconds(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid window: " + window + ". Use formats like 7d, 24h, 30m, 10s");
        }
        throw new IllegalArgumentException("Invalid window: " + window + ". Use formats like 7d, 24h, 30m, 10s");
    }
}
