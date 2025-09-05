package com.ibarkhatov.urlshortener.repository;

import com.ibarkhatov.urlshortener.domain.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
}
