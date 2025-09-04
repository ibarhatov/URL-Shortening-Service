package com.ibarkhatov.urlshortener.service;

import com.ibarkhatov.urlshortener.domain.ShortUrl;
import com.ibarkhatov.urlshortener.dto.CreateUrlRequest;
import com.ibarkhatov.urlshortener.dto.UrlResponse;
import com.ibarkhatov.urlshortener.repository.ShortUrlRepository;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.nio.ByteBuffer;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class UrlShorteningServiceTest {

    @Test
    void createShortUrl_generatesUrlSafeBase64CodeFromId() {
        ShortUrlRepository repository = Mockito.mock(ShortUrlRepository.class);
        UrlShorteningService service = new UrlShorteningService(repository);

        long newId = 123L;
        String originalUrl = "https://originalurl.com/page";
        CreateUrlRequest request = new CreateUrlRequest(originalUrl);

        ArgumentMatcher<ShortUrl> firstSave = e -> e != null && e.getId() == null && e.getShortCode() == null;
        when(repository.save(argThat(firstSave))).thenAnswer(inv -> {
            ShortUrl in = inv.getArgument(0);
            ShortUrl out = new ShortUrl();
            out.setId(newId);
            out.setOriginalUrl(in.getOriginalUrl());
            out.setClickCount(in.getClickCount());
            out.setCreatedAt(Instant.now());
            return out;
        });

        ArgumentMatcher<ShortUrl> secondSave = e -> e != null && e.getId() != null && e.getShortCode() != null;
        when(repository.save(argThat(secondSave))).thenAnswer(inv -> inv.getArgument(0));

        UrlResponse response = service.createShortUrl(request);

        byte[] idBytes = ByteBuffer.allocate(Long.BYTES).putLong(newId).array();
        String expectedCode = Base64.encodeBase64URLSafeString(idBytes);

        assertThat(response.id()).isEqualTo(newId);
        assertThat(response.originalUrl()).isEqualTo(originalUrl);
        assertThat(response.shortCode()).isEqualTo(expectedCode);
        assertThat(response.clickCount()).isZero();
        assertThat(response.createdAt()).isNotNull();

        Mockito.verify(repository, times(2)).save(any(ShortUrl.class));
    }
}


