package ru.practicum.location.service;

import java.util.List;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.location.dto.NamedLocationDto;
import ru.practicum.location.dto.NewLocationDto;
import ru.practicum.location.dto.UpdateLocationDto;

public interface LocationService {

    NamedLocationDto addLocation(NewLocationDto dto);

    NamedLocationDto updateLocation(Long locationId, UpdateLocationDto dto);

    void deleteLocation(Long locationId);

    List<NamedLocationDto> getLocations(int from, int size);

    NamedLocationDto getLocation(Long locationId);

    List<EventShortDto> getEventsInLocation(Long locationId, int from, int size);
}
