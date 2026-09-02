package ru.practicum.location.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.location.dto.NamedLocationDto;
import ru.practicum.location.dto.NewLocationDto;
import ru.practicum.location.model.NamedLocation;

@Component
public class LocationMapper {

    public NamedLocation toEntity(NewLocationDto dto) {
        return NamedLocation.builder()
                .name(dto.getName())
                .lat(dto.getLat())
                .lon(dto.getLon())
                .radius(dto.getRadius())
                .build();
    }

    public NamedLocationDto toDto(NamedLocation location) {
        return NamedLocationDto.builder()
                .id(location.getId())
                .name(location.getName())
                .lat(location.getLat())
                .lon(location.getLon())
                .radius(location.getRadius())
                .build();
    }
}
