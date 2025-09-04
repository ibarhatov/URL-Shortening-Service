package com.ibarkhatov.urlshortener.mapper;

import com.ibarkhatov.urlshortener.domain.ShortUrl;
import com.ibarkhatov.urlshortener.dto.UrlResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShortUrlMapper {
    UrlResponse toDto(ShortUrl entity);
}
