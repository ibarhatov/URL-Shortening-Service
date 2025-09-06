package com.ibarkhatov.urlshortener.mapper;

import com.ibarkhatov.urlshortener.dto.TopUrlResponse;
import com.ibarkhatov.urlshortener.repository.projection.TopItemView;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TopUrlMapper {
    List<TopUrlResponse> toDtoList(List<TopItemView> views);
}
