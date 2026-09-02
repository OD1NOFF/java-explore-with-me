package ru.practicum.request.mapper;

import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.Request;

@Component
public class RequestMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ParticipationRequestDto toDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated().format(FORMATTER))
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus().name())
                .build();
    }
}
