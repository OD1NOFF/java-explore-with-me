package ru.practicum.request.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.service.RequestService;

/**
 * Заявки на участие в конкретном событии — с точки зрения его инициатора.
 * URL начинается с /users/{userId}/events/{eventId}/requests, поэтому вынесено
 * из основного PrivateEventController, но логика целиком про Request.
 */
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class EventParticipationController {

    private final RequestService service;

    @GetMapping
    public List<ParticipationRequestDto> getEventParticipants(@PathVariable Long userId, @PathVariable Long eventId) {
        return service.getEventParticipants(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult changeRequestStatus(@PathVariable Long userId, @PathVariable Long eventId,
                                                                @RequestBody @Valid EventRequestStatusUpdateRequest dto) {
        return service.changeRequestStatus(userId, eventId, dto);
    }
}
