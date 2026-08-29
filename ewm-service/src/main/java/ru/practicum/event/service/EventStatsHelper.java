package ru.practicum.event.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.event.model.Event;
import ru.practicum.request.repository.EventConfirmedCount;
import ru.practicum.request.repository.RequestRepository;

/**
 * Батч-загрузка "количества просмотров" (из сервиса статистики) и "количества
 * подтверждённых заявок" (из БД) для списка событий одним запросом на список,
 * а не по запросу на каждое событие.
 */
@Component
@RequiredArgsConstructor
public class EventStatsHelper {

    private static final LocalDateTime EPOCH = LocalDateTime.of(2000, 1, 1, 0, 0, 0);

    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    public Map<Long, Long> confirmedRequests(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> result = new HashMap<>();
        for (EventConfirmedCount row : requestRepository.countConfirmedByEventIds(ids)) {
            result.put(row.getEventId(), row.getCount());
        }
        return result;
    }

    public Map<Long, Long> views(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<String> uris = events.stream().map(e -> "/events/" + e.getId()).toList();
        List<ViewStatsDto> stats = statsClient.getStats(EPOCH, LocalDateTime.now(), uris, true);

        Map<Long, Long> result = new HashMap<>();
        for (ViewStatsDto stat : stats) {
            String uri = stat.getUri();
            Long eventId = Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
            result.put(eventId, stat.getHits());
        }
        return result;
    }
}
