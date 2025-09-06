package com.ibarkhatov.urlshortener.service;

import com.ibarkhatov.urlshortener.domain.ShortUrl;
import com.ibarkhatov.urlshortener.dto.CreateUrlRequest;
import com.ibarkhatov.urlshortener.dto.TopUrlResponse;
import com.ibarkhatov.urlshortener.dto.UrlResponse;
import com.ibarkhatov.urlshortener.mapper.ShortUrlMapper;
import com.ibarkhatov.urlshortener.mapper.TopUrlMapper;
import com.ibarkhatov.urlshortener.repository.ShortUrlRepository;
import com.ibarkhatov.urlshortener.repository.projection.ShortUrlClickView;
import com.ibarkhatov.urlshortener.repository.projection.TopItemView;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class UrlShorteningServiceTest {

    private ShortUrlRepository repository;
    private ShortUrlMapper mapper;
    private TopUrlMapper topUrlMapper;
    private UrlShorteningService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ShortUrlRepository.class);
        mapper = Mockito.mock(ShortUrlMapper.class);
        topUrlMapper = Mockito.mock(TopUrlMapper.class);
        service = new UrlShorteningService(repository, mapper, topUrlMapper);
    }

    @Test
    void createShortUrl_generatesUrlSafeBase64CodeFromId() {
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
        String code = "abc";
        String original = "https://original.com";

        var view = Mockito.mock(ShortUrlClickView.class);
        Mockito.when(view.getId()).thenReturn(10L);
        Mockito.when(view.getOriginalUrl()).thenReturn(original);
        Mockito.when(repository.incrementAndReturn(code)).thenReturn(Optional.of(view));

        Optional<String> result = service.resolveAndTrack(code);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(original);
        Mockito.verify(repository).incrementAndReturn(code);
    }

    @Test
    void resolveAndTrack_returnsEmpty_whenNotFound() {
        Mockito.when(repository.incrementAndReturn("missing")).thenReturn(Optional.empty());

        Optional<String> result = service.resolveAndTrack("missing");

        assertThat(result).isEmpty();
        Mockito.verify(repository).incrementAndReturn("missing");
    }

    @Test
    void listAll_returnsMappedResponses() {
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

        Mockito.when(repository.findAll()).thenReturn(of(e1, e2));
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

    @Test
    void deleteById_returnsTrue_whenEntityExists() {
        Long id = 42L;
        Mockito.when(repository.existsById(id)).thenReturn(true);

        boolean result = service.deleteById(id);

        assertThat(result).isTrue();
        Mockito.verify(repository).existsById(id);
        Mockito.verify(repository).deleteById(id);
    }

    @Test
    void deleteById_returnsFalse_whenEntityMissing() {
        Long id = 100L;
        Mockito.when(repository.existsById(id)).thenReturn(false);

        boolean result = service.deleteById(id);

        assertThat(result).isFalse();
        Mockito.verify(repository).existsById(id);
        Mockito.verify(repository, Mockito.never()).deleteById(Mockito.anyLong());
    }

    @Test
    void getTopByWindow_nullWindow_usesAllTime() {
        int limit = 3;
        TopItemView v1 = Mockito.mock(TopItemView.class);
        TopItemView v2 = Mockito.mock(TopItemView.class);
        var views = of(v1, v2);
        Mockito.when(repository.findTopByClickCount(limit)).thenReturn(views);

        TopUrlResponse r1 = new TopUrlResponse("a", "https://a", 10, null);
        TopUrlResponse r2 = new TopUrlResponse("b", "https://b", 5, null);
        Mockito.when(topUrlMapper.toDtoList(views)).thenReturn(of(r1, r2));

        var result = service.getTopByWindow(null, limit);

        assertThat(result).containsExactly(r1, r2);
        Mockito.verify(repository).findTopByClickCount(limit);
        Mockito.verify(repository, Mockito.never())
                .findTopByWindow(Mockito.any(), Mockito.anyInt());
        Mockito.verify(topUrlMapper).toDtoList(views);
    }

    @Test
    void getTopByWindow_withDuration_usesWindowQuery() {
        int limit = 2;
        TopItemView v = Mockito.mock(TopItemView.class);
        var views = of(v);
        Mockito.when(repository.findTopByWindow(Mockito.any(), Mockito.eq(limit))).thenReturn(views);

        TopUrlResponse r = new TopUrlResponse("x", "https://x", 7, null);
        Mockito.when(topUrlMapper.toDtoList(views)).thenReturn(of(r));

        var result = service.getTopByWindow("7d", limit);

        assertThat(result).containsExactly(r);
        Mockito.verify(repository).findTopByWindow(Mockito.any(), Mockito.eq(limit));
        Mockito.verify(repository, Mockito.never()).findTopByClickCount(Mockito.anyInt());
        Mockito.verify(topUrlMapper).toDtoList(views);
    }
}
