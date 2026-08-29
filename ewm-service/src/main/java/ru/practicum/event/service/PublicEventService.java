package ru.practicum.event.service;

import java.time.LocalDateTime;
import java.util.List;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.EventSort;

public interface PublicEventService {

    List<EventShortDto> searchEvents(String text, List<Long> categories, Boolean paid,
                                      LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                      boolean onlyAvailable, EventSort sort, int from, int size,
                                      String ip, String uri);

    EventFullDto getEvent(Long id, String ip, String uri);
}
