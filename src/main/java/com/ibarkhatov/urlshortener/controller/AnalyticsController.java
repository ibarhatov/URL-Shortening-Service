package com.ibarkhatov.urlshortener.controller;

import com.ibarkhatov.urlshortener.dto.TopUrlResponse;
import com.ibarkhatov.urlshortener.service.UrlShorteningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final UrlShorteningService service;

    @GetMapping("/top")
    public ResponseEntity<List<TopUrlResponse>> top(
            @RequestParam(name = "window", required = false) String window,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit
    ) {
        log.info("Analytics top: window={}, limit={}", window, limit);
        List<TopUrlResponse> result = service.getTopByWindow(window, limit);
        log.info("Analytics top size={}", result.size());
        return ResponseEntity.ok(result);
    }
}
