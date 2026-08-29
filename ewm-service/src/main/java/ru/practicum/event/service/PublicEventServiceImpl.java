package ru.practicum.event.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.EventSort;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.EventSpecifications;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.util.AppConstants;
import ru.practicum.util.OffsetPageRequest;

@Service
@RequiredArgsConstructor
public class PublicEventServiceImpl implements PublicEventService {

    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final EventMapper mapper;
    private final EventStatsHelper statsHelper;
    private final StatsClient statsClient;

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> searchEvents(String text, List<Long> categories, Boolean paid,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                             boolean onlyAvailable, EventSort sort, int from, int size,
                                             String ip, String uri) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("rangeStart must not be after rangeEnd");
        }

        LocalDateTime effectiveStart = rangeStart;
        LocalDateTime effectiveEnd = rangeEnd;
        if (rangeStart == null && rangeEnd == null) {
            effectiveStart = LocalDateTime.now();
        }

        Specification<Event> spec = Specification.<Event>where(EventSpecifications.hasState(EventState.PUBLISHED))
                .and(EventSpecifications.hasCategories(categories))
                .and(EventSpecifications.isPaid(paid))
                .and(EventSpecifications.eventDateAfter(effectiveStart))
                .and(EventSpecifications.eventDateBefore(effectiveEnd))
                .and(EventSpecifications.textContains(text));
        if (onlyAvailable) {
            spec = spec.and(EventSpecifications.onlyAvailable());
        }

        List<Event> events = eventRepository.findAll(spec, OffsetPageRequest.of(from, size, Sort.by("eventDate")))
                .getContent();

        statsClient.saveHit(AppConstants.APP_NAME, uri, ip, LocalDateTime.now());

        Map<Long, Long> confirmed = statsHelper.confirmedRequests(events);
        Map<Long, Long> views = statsHelper.views(events);

        List<EventShortDto> result = events.stream()
                .map(e -> mapper.toShortDto(e, confirmed.getOrDefault(e.getId(), 0L), views.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toCollection(ArrayList::new));

        // Сортировка по просмотрам применяется в памяти к уже полученной странице:
        // счётчик просмотров живёт в отдельном сервисе статистики, а не в БД,
        // поэтому глобальная сортировка по нему на уровне SQL недоступна.
        if (sort == EventSort.VIEWS) {
            result.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEvent(Long id, String ip, String uri) {
        Event event = eventRepository.findById(id)
                .filter(e -> e.getState() == EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        statsClient.saveHit(AppConstants.APP_NAME, uri, ip, LocalDateTime.now());

        long confirmed = requestRepository.countByEventIdAndStatus(id, RequestStatus.CONFIRMED);
        Map<Long, Long> views = statsHelper.views(List.of(event));

        return mapper.toFullDto(event, confirmed, views.getOrDefault(id, 0L));
    }
}
