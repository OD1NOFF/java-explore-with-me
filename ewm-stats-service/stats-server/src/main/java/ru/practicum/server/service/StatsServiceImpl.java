package ru.practicum.server.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.server.exception.ValidationException;
import ru.practicum.server.mapper.StatsMapper;
import ru.practicum.server.model.EndpointHit;
import ru.practicum.server.repository.StatsRepository;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository repository;
    private final StatsMapper mapper;

    @Override
    @Transactional
    public EndpointHitDto saveHit(EndpointHitDto hitDto) {
        EndpointHit hit = mapper.toEntity(hitDto);
        EndpointHit saved = repository.save(hit);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new ValidationException("start date must not be after end date");
        }

        boolean noUrisFilter = (uris == null || uris.isEmpty());

        if (noUrisFilter) {
            return unique ? repository.findAllUniqueHits(start, end) : repository.findAllHits(start, end);
        }
        return unique
                ? repository.findUniqueHitsByUris(start, end, uris)
                : repository.findHitsByUris(start, end, uris);
    }
}
