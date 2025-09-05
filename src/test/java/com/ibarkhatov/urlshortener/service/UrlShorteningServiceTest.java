package com.ibarkhatov.urlshortener.service;

import com.ibarkhatov.urlshortener.domain.ShortUrl;
import com.ibarkhatov.urlshortener.dto.CreateUrlRequest;
import com.ibarkhatov.urlshortener.dto.UrlResponse;
import com.ibarkhatov.urlshortener.mapper.ShortUrlMapper;
import com.ibarkhatov.urlshortener.repository.ShortUrlRepository;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class UrlShorteningServiceTest {

    @Test
    void createShortUrl_generatesUrlSafeBase64CodeFromId() {
        ShortUrlRepository repository = Mockito.mock(ShortUrlRepository.class);
        ShortUrlMapper mapper = Mockito.mock(ShortUrlMapper.class);
        UrlShorteningService service = new UrlShorteningService(repository, mapper);

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

        Mockito.when(mapper.toDto(Mockito.any(ShortUrl.class))).thenAnswer(inv -> {
            ShortUrl e = inv.getArgument(0);
            return new UrlResponse(e.getId(), e.getOriginalUrl(), e.getShortCode(), e.getCreatedAt(), e.getClickCount(), e.getLastAccessedAt());
        });

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

    @Test
    void resolveAndTrack_updatesMetrics_andReturnsOriginalUrl() {
        ShortUrlRepository repository = Mockito.mock(ShortUrlRepository.class);
        ShortUrlMapper mapper = Mockito.mock(ShortUrlMapper.class);
        UrlShorteningService service = new UrlShorteningService(repository, mapper);

        String code = "abc";
        String original = "https://original.com";

        ShortUrl entity = new ShortUrl();
        entity.setId(10L);
        entity.setOriginalUrl(original);
        entity.setClickCount(7);
        entity.setCreatedAt(Instant.now());

        Mockito.when(repository.findByShortCode(code)).thenReturn(Optional.of(entity));
        Mockito.when(repository.save(Mockito.any(ShortUrl.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<String> result = service.resolveAndTrack(code);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(original);

        Mockito.verify(repository).save(argThat(e ->
                e.getClickCount() == 8 &&
                        e.getLastAccessedAt() != null &&
                        original.equals(e.getOriginalUrl())
        ));
    }

    @Test
    void resolveAndTrack_returnsEmpty_whenNotFound() {
        ShortUrlRepository repository = Mockito.mock(ShortUrlRepository.class);
        ShortUrlMapper mapper = Mockito.mock(ShortUrlMapper.class);
        UrlShorteningService service = new UrlShorteningService(repository, mapper);

        Mockito.when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        Optional<String> result = service.resolveAndTrack("missing");

        assertThat(result).isEmpty();
        Mockito.verify(repository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void listAll_returnsMappedResponses() {
        ShortUrlRepository repository = Mockito.mock(ShortUrlRepository.class);
        ShortUrlMapper mapper = Mockito.mock(ShortUrlMapper.class);
        UrlShorteningService service = new UrlShorteningService(repository, mapper);

        ShortUrl e1 = new ShortUrl();
        e1.setId(1L);
        e1.setOriginalUrl("https://a.com");
        e1.setShortCode("AAA");
        e1.setCreatedAt(Instant.now());
        e1.setClickCount(3);

        ShortUrl e2 = new ShortUrl();
        e2.setId(2L);
        e2.setOriginalUrl("https://b.com");
        e2.setShortCode("BBB");
        e2.setCreatedAt(Instant.now());
        e2.setClickCount(5);

        Mockito.when(repository.findAll()).thenReturn(List.of(e1, e2));
        Mockito.when(mapper.toDto(Mockito.any(ShortUrl.class))).thenAnswer(inv -> {
            ShortUrl x = inv.getArgument(0);
            return new UrlResponse(x.getId(), x.getOriginalUrl(), x.getShortCode(), x.getCreatedAt(), x.getClickCount(), x.getLastAccessedAt());
        });

        List<UrlResponse> list = service.listAll();

        assertThat(list).hasSize(2);
        assertThat(list.get(0).id()).isEqualTo(1L);
        assertThat(list.get(0).originalUrl()).isEqualTo("https://a.com");
        assertThat(list.get(0).shortCode()).isEqualTo("AAA");
        assertThat(list.get(1).id()).isEqualTo(2L);
        assertThat(list.get(1).originalUrl()).isEqualTo("https://b.com");
        assertThat(list.get(1).shortCode()).isEqualTo("BBB");

        Mockito.verify(repository).findAll();
        Mockito.verify(mapper, Mockito.times(2)).toDto(Mockito.any(ShortUrl.class));
    }
}
