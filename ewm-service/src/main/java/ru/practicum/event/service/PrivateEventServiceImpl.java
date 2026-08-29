package ru.practicum.event.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.dto.UserStateAction;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;
import ru.practicum.util.AppConstants;
import ru.practicum.util.OffsetPageRequest;

@Service
@RequiredArgsConstructor
public class PrivateEventServiceImpl implements PrivateEventService {

    private static final int MIN_HOURS_BEFORE_EVENT = 2;

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper mapper;
    private final EventStatsHelper statsHelper;

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        User initiator = getUserOrThrow(userId);
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found"));

        LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate(), AppConstants.DATE_TIME_FORMATTER);
        validateEventDate(eventDate, MIN_HOURS_BEFORE_EVENT, dto.getEventDate());

        Event event = mapper.toEntity(dto, category, initiator);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        Event saved = eventRepository.save(event);
        return mapper.toFullDto(saved, 0L, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        getUserOrThrow(userId);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, OffsetPageRequest.of(from, size)).getContent();

        Map<Long, Long> confirmed = statsHelper.confirmedRequests(events);
        Map<Long, Long> views = statsHelper.views(events);

        return events.stream()
                .map(e -> mapper.toShortDto(e, confirmed.getOrDefault(e.getId(), 0L), views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = getOwnedEventOrThrow(userId, eventId);
        Map<Long, Long> confirmed = statsHelper.confirmedRequests(List.of(event));
        Map<Long, Long> views = statsHelper.views(List.of(event));
        return mapper.toFullDto(event, confirmed.getOrDefault(eventId, 0L), views.getOrDefault(eventId, 0L));
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event event = getOwnedEventOrThrow(userId, eventId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (dto.getEventDate() != null) {
            LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate(), AppConstants.DATE_TIME_FORMATTER);
            validateEventDate(eventDate, MIN_HOURS_BEFORE_EVENT, dto.getEventDate());
            event.setEventDate(eventDate);
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
        if (dto.getStateAction() == UserStateAction.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        } else if (dto.getStateAction() == UserStateAction.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }

        Map<Long, Long> confirmed = statsHelper.confirmedRequests(List.of(event));
        Map<Long, Long> views = statsHelper.views(List.of(event));
        return mapper.toFullDto(event, confirmed.getOrDefault(eventId, 0L), views.getOrDefault(eventId, 0L));
    }

    private void validateEventDate(LocalDateTime eventDate, int minHours, String rawValue) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(minHours))) {
            throw new ConflictException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. "
                    + "Value: " + rawValue);
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Event getOwnedEventOrThrow(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }
}
