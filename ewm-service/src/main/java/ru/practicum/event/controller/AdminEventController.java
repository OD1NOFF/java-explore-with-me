package ru.practicum.event.controller;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.event.service.AdminEventService;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final AdminEventService service;

    @GetMapping
    public List<EventFullDto> searchEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = TIMESTAMP_PATTERN) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = TIMESTAMP_PATTERN) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        return service.searchEvents(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable Long eventId, @RequestBody @Valid UpdateEventAdminRequest dto) {
        return service.updateEvent(eventId, dto);
    }
}
