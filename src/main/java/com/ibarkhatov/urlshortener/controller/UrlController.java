package com.ibarkhatov.urlshortener.controller;

import com.ibarkhatov.urlshortener.dto.CreateUrlRequest;
import com.ibarkhatov.urlshortener.dto.UrlResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping
public class UrlController {

    @PostMapping("/urls")
    public ResponseEntity<UrlResponse> createShortUrl(@Valid @RequestBody CreateUrlRequest request) {
        UrlResponse response = new UrlResponse(
                1L,
                request.originalUrl(),
                "stub123",
                Instant.now(),
                0L,
                null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        HttpHeaders headers = new HttpHeaders();
        String originalUrl = "https://mock.com";
        headers.setLocation(URI.create(originalUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
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


