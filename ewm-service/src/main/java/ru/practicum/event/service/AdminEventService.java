package ru.practicum.event.service;

import java.time.LocalDateTime;
import java.util.List;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;

public interface AdminEventService {

    List<EventFullDto> searchEvents(List<Long> users, List<String> states, List<Long> categories,
                                     LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest dto);
}
