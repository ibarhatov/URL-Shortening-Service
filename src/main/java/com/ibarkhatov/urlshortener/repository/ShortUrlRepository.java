package com.ibarkhatov.urlshortener.repository;

import com.ibarkhatov.urlshortener.domain.ShortUrl;
import com.ibarkhatov.urlshortener.repository.projection.ShortUrlClickView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
    Optional<ShortUrl> findByShortCode(String shortCode);

    @Modifying
    @Query(value = """
            update short_url
            set click_count = click_count + 1,
                last_accessed_at = now()
            where short_code = :code
            returning id as id, original_url as originalUrl
            """, nativeQuery = true)
    Optional<ShortUrlClickView> incrementAndReturn(@Param("code") String code);
}
