package ru.practicum.server.service;

import java.time.LocalDateTime;
import java.util.List;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

public interface StatsService {

    EndpointHitDto saveHit(EndpointHitDto hitDto);

    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
