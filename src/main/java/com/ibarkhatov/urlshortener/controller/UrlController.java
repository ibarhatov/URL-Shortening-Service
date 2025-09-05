package com.ibarkhatov.urlshortener.controller;

import com.ibarkhatov.urlshortener.dto.CreateUrlRequest;
import com.ibarkhatov.urlshortener.dto.UrlResponse;
import com.ibarkhatov.urlshortener.service.UrlShorteningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class UrlController {

    private final UrlShorteningService service;

    @PostMapping("/urls")
    public ResponseEntity<UrlResponse> createShortUrl(@Valid @RequestBody CreateUrlRequest request) {
        log.info("Create url");
        UrlResponse response = service.createShortUrl(request);
        log.info("Created id={}, code={}", response.id(), response.shortCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        log.info("Redirect by {}", shortCode);
        return service.resolveAndTrack(shortCode)
                .map(url -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setLocation(URI.create(url));
                    log.info("Redirecting");
                    return new ResponseEntity<Void>(headers, HttpStatus.FOUND);
                })
                .orElseGet(() -> {
                    log.warn("Not found {}", shortCode);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }

    @GetMapping("/urls")
    public ResponseEntity<List<UrlResponse>> listAll() {
        List<UrlResponse> list = service.listAll();
        log.info("List urls: {}", list.size());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/urls/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Delete {}", id);
        boolean removed = service.deleteById(id);
        if (removed) {
            log.info("Deleted {}", id);
            return ResponseEntity.noContent().build();
        } else {
            log.warn("Not found {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
