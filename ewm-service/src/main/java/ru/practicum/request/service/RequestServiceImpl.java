package ru.practicum.request.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestMapper mapper;

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Event initiator can't add a request to participate in their own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in an unpublished event");
        }
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request already exists");
        }
        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }

        RequestStatus status = (event.getParticipantLimit() == 0 || !event.getRequestModeration())
                ? RequestStatus.CONFIRMED
                : RequestStatus.PENDING;

        Request request = Request.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(status)
                .build();

        return mapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        return requestRepository.findAllByRequesterId(userId).stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findById(requestId)
                .filter(r -> r.getRequester().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));
        request.setStatus(RequestStatus.CANCELED);
        return mapper.toDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        getOwnedEventOrThrow(userId, eventId);
        return requestRepository.findAllByEventId(eventId).stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                                EventRequestStatusUpdateRequest dto) {
        Event event = getOwnedEventOrThrow(userId, eventId);

        RequestStatus target;
        try {
            target = RequestStatus.valueOf(dto.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Unknown request status: " + dto.getStatus());
        }
        if (target != RequestStatus.CONFIRMED && target != RequestStatus.REJECTED) {
            throw new ValidationException("Status must be CONFIRMED or REJECTED");
        }

        List<Request> requests = requestRepository.findAllByEventIdAndIdIn(eventId, dto.getRequestIds());
        if (requests.size() != dto.getRequestIds().size()) {
            throw new NotFoundException("Some of the requests were not found for event with id=" + eventId);
        }
        for (Request request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (target == RequestStatus.REJECTED) {
            for (Request request : requests) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(mapper.toDto(request));
            }
            return new EventRequestStatusUpdateResult(confirmed, rejected);
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        int limit = event.getParticipantLimit();

        for (Request request : requests) {
            if (limit > 0 && confirmedCount >= limit) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(mapper.toDto(request));
            } else {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedCount++;
                confirmed.add(mapper.toDto(request));
            }
        }

        if (limit > 0 && confirmedCount >= limit) {
            for (Request stillPending : requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.PENDING)) {
                stillPending.setStatus(RequestStatus.REJECTED);
                rejected.add(mapper.toDto(stillPending));
            }
        }

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    private Event getOwnedEventOrThrow(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }
}
