package ru.practicum.compilation.mapper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.service.EventStatsHelper;

@Component
@RequiredArgsConstructor
public class CompilationMapper {

    private final EventMapper eventMapper;
    private final EventStatsHelper statsHelper;

    public Compilation toEntity(NewCompilationDto dto, List<Event> events) {
        Compilation compilation = Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned() == null ? Boolean.FALSE : dto.getPinned())
                .build();
        compilation.setEvents(new HashSet<>(events));
        return compilation;
    }

    public CompilationDto toDto(Compilation compilation) {
        List<Event> events = List.copyOf(compilation.getEvents());
        Map<Long, Long> confirmed = statsHelper.confirmedRequests(events);
        Map<Long, Long> views = statsHelper.views(events);

        List<EventShortDto> eventDtos = events.stream()
                .map(e -> eventMapper.toShortDto(e, confirmed.getOrDefault(e.getId(), 0L), views.getOrDefault(e.getId(), 0L)))
                .toList();

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(eventDtos)
                .build();
    }
}
