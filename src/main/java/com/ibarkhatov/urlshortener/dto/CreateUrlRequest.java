package com.ibarkhatov.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUrlRequest(
        @NotBlank String originalUrl
) {
}


