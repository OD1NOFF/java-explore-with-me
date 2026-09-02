package ru.practicum.event.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dto.AdminStateAction;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.EventSpecifications;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.util.AppConstants;
import ru.practicum.util.OffsetPageRequest;

@Service
@RequiredArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {

    private static final int MIN_HOURS_BEFORE_PUBLISH = 1;

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper mapper;
    private final EventStatsHelper statsHelper;

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> searchEvents(List<Long> users, List<String> states, List<Long> categories,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        List<EventState> eventStates = states == null ? null : states.stream().map(EventState::valueOf).toList();

        Specification<Event> spec = Specification.<Event>where(EventSpecifications.hasInitiators(users))
                .and(EventSpecifications.hasStates(eventStates))
                .and(EventSpecifications.hasCategories(categories))
                .and(EventSpecifications.eventDateAfter(rangeStart))
                .and(EventSpecifications.eventDateBefore(rangeEnd));

        List<Event> events = eventRepository.findAll(spec, OffsetPageRequest.of(from, size, Sort.by("eventDate"))).getContent();

        Map<Long, Long> confirmed = statsHelper.confirmedRequests(events);
        Map<Long, Long> views = statsHelper.views(events);

        return events.stream()
                .map(e -> mapper.toFullDto(e, confirmed.getOrDefault(e.getId(), 0L), views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (dto.getEventDate() != null) {
            LocalDateTime newEventDate = LocalDateTime.parse(dto.getEventDate(), AppConstants.DATE_TIME_FORMATTER);
            if (newEventDate.isBefore(LocalDateTime.now())) {
                throw new ValidationException("Field: eventDate. Error: должно содержать дату, которая еще не "
                        + "наступила. Value: " + dto.getEventDate());
            }
            event.setEventDate(newEventDate);
        }
        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found"));
            event.setCategory(category);
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getLocation() != null) {
            event.setLocation(new Location(dto.getLocation().getLat(), dto.getLocation().getLon()));
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }

        if (dto.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in the right state: "
                        + event.getState());
            }
            if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_PUBLISH))) {
                throw new ConflictException("Field: eventDate. Error: дата начала события должна быть не ранее "
                        + "чем за час от даты публикации. Value: " + event.getEventDate());
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (dto.getStateAction() == AdminStateAction.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Cannot reject already published event");
            }
            event.setState(EventState.CANCELED);
        }

        Map<Long, Long> confirmed = statsHelper.confirmedRequests(List.of(event));
        Map<Long, Long> views = statsHelper.views(List.of(event));
        return mapper.toFullDto(event, confirmed.getOrDefault(eventId, 0L), views.getOrDefault(eventId, 0L));
    }
}
