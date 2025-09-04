package com.ibarkhatov.urlshortener.controller;

import com.ibarkhatov.urlshortener.dto.CreateUrlRequest;
import com.ibarkhatov.urlshortener.dto.UrlResponse;
import com.ibarkhatov.urlshortener.service.UrlShorteningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UrlController {

    private final UrlShorteningService service;

    @PostMapping("/urls")
    public ResponseEntity<UrlResponse> createShortUrl(@Valid @RequestBody CreateUrlRequest request) {
        UrlResponse response = service.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        return service.trackClickAndResolve(shortCode)
                .map(url -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setLocation(URI.create(url));
                    return new ResponseEntity<Void>(headers, HttpStatus.FOUND);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/urls")
    public ResponseEntity<List<UrlResponse>> listAll() {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @DeleteMapping("/urls/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }
}
