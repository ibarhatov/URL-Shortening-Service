package com.ibarkhatov.urlshortener.repository;

import com.ibarkhatov.urlshortener.domain.ShortUrl;
import com.ibarkhatov.urlshortener.repository.projection.ShortUrlClickView;
import com.ibarkhatov.urlshortener.repository.projection.TopItemView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
    Optional<ShortUrl> findByShortCode(String shortCode);

    @Modifying
    @Query(value = """
            with updated as (
                update short_url
                set click_count = click_count + 1,
                    last_accessed_at = now()
                where short_code = :code
                returning id, last_accessed_at, original_url
            ), ins as (
                insert into short_url_access(short_url_id, accessed_at)
                select id, last_accessed_at from updated
            )
            select id as id, original_url as originalUrl from updated
            """, nativeQuery = true)
    Optional<ShortUrlClickView> incrementAndReturn(@Param("code") String code);

    @Query(value = """
            select s.short_code as shortCode,
                   s.original_url as originalUrl,
                   s.click_count as click_count,
                   s.last_accessed_at as lastAccessedAt
            from short_url s
            order by s.click_count desc
            limit :limit
            """, nativeQuery = true)
    List<TopItemView> findTopByClickCount(@Param("limit") int limit);

    @Query(value = """
            select s.short_code as shortCode,
                   s.original_url as originalUrl,
                   count(a.id) as clickCount,
                   max(a.accessed_at) as lastAccessedAt
            from short_url s
            join short_url_access a
              on a.short_url_id = s.id
             and a.accessed_at >= :fromTs
            group by s.short_code, s.original_url
            order by clickCount desc
            limit :limit
            """, nativeQuery = true)
    List<TopItemView> findTopByWindow(@Param("fromTs") OffsetDateTime fromTs, @Param("limit") int limit);
}
