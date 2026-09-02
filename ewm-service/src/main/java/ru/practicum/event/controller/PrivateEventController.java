package ru.practicum.event.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.service.PrivateEventService;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventController {

    private final PrivateEventService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@PathVariable Long userId, @RequestBody @Valid NewEventDto dto) {
        return service.addEvent(userId, dto);
    }

    @GetMapping
    public List<EventShortDto> getUserEvents(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "0") int from,
                                              @RequestParam(defaultValue = "10") int size) {
        return service.getUserEvents(userId, from, size);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getUserEvent(@PathVariable Long userId, @PathVariable Long eventId) {
        return service.getUserEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateUserEvent(@PathVariable Long userId, @PathVariable Long eventId,
                                         @RequestBody @Valid UpdateEventUserRequest dto) {
        return service.updateUserEvent(userId, eventId, dto);
    }
}
