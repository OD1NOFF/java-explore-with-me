package ru.practicum.event.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;

@Component
@RequiredArgsConstructor
public class EventMapper {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public Event toEntity(NewEventDto dto, Category category, User initiator) {
        return Event.builder()
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .category(category)
                .initiator(initiator)
                .eventDate(LocalDateTime.parse(dto.getEventDate(), DATE_FORMATTER))
                .location(new Location(dto.getLocation().getLat(), dto.getLocation().getLon()))
                .paid(dto.getPaid() == null ? Boolean.FALSE : dto.getPaid())
                .participantLimit(dto.getParticipantLimit() == null ? 0 : dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration() == null ? Boolean.TRUE : dto.getRequestModeration())
                .build();
    }

    public EventFullDto toFullDto(Event event, long confirmedRequests, long views) {
        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .category(categoryMapper.toDto(event.getCategory()))
                .initiator(userMapper.toShortDto(event.getInitiator()))
                .location(new LocationDto(event.getLocation().getLat(), event.getLocation().getLon()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(event.getState().name())
                .createdOn(event.getCreatedOn().format(DATE_FORMATTER))
                .publishedOn(event.getPublishedOn() == null ? null : event.getPublishedOn().format(DATE_FORMATTER))
                .eventDate(event.getEventDate().format(DATE_FORMATTER))
                .confirmedRequests(confirmedRequests)
                .views(views)
                .build();
    }

    public EventShortDto toShortDto(Event event, long confirmedRequests, long views) {
        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toDto(event.getCategory()))
                .initiator(userMapper.toShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .eventDate(event.getEventDate().format(DATE_FORMATTER))
                .confirmedRequests(confirmedRequests)
                .views(views)
                .build();
    }
}
