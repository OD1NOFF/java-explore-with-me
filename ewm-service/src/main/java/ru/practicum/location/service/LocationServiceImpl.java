package ru.practicum.location.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.service.EventStatsHelper;
import ru.practicum.exception.NotFoundException;
import ru.practicum.location.dto.NamedLocationDto;
import ru.practicum.location.dto.NewLocationDto;
import ru.practicum.location.dto.UpdateLocationDto;
import ru.practicum.location.mapper.LocationMapper;
import ru.practicum.location.model.NamedLocation;
import ru.practicum.location.repository.NamedLocationRepository;
import ru.practicum.util.OffsetPageRequest;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final NamedLocationRepository repository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventStatsHelper statsHelper;
    private final LocationMapper mapper;

    @Override
    @Transactional
    public NamedLocationDto addLocation(NewLocationDto dto) {
        NamedLocation saved = repository.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public NamedLocationDto updateLocation(Long locationId, UpdateLocationDto dto) {
        NamedLocation location = getOrThrow(locationId);
        if (dto.getName() != null) {
            location.setName(dto.getName());
        }
        if (dto.getLat() != null) {
            location.setLat(dto.getLat());
        }
        if (dto.getLon() != null) {
            location.setLon(dto.getLon());
        }
        if (dto.getRadius() != null) {
            location.setRadius(dto.getRadius());
        }
        return mapper.toDto(location);
    }

    @Override
    @Transactional
    public void deleteLocation(Long locationId) {
        getOrThrow(locationId);
        repository.deleteById(locationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NamedLocationDto> getLocations(int from, int size) {
        return repository.findAll(OffsetPageRequest.of(from, size)).getContent().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NamedLocationDto getLocation(Long locationId) {
        return mapper.toDto(getOrThrow(locationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsInLocation(Long locationId, int from, int size) {
        NamedLocation location = getOrThrow(locationId);

        List<Event> events = eventRepository.findPublishedEventsWithinRadius(
                location.getLat(), location.getLon(), location.getRadius(), OffsetPageRequest.of(from, size));

        Map<Long, Long> confirmed = statsHelper.confirmedRequests(events);
        Map<Long, Long> views = statsHelper.views(events);

        return events.stream()
                .map(e -> eventMapper.toShortDto(e, confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private NamedLocation getOrThrow(Long locationId) {
        return repository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location with id=" + locationId + " was not found"));
    }
}
