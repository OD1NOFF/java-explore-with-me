package ru.practicum.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.server.exception.ValidationException;
import ru.practicum.server.mapper.StatsMapper;
import ru.practicum.server.model.EndpointHit;
import ru.practicum.server.repository.StatsRepository;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private StatsRepository repository;

    private final StatsMapper mapper = new StatsMapper();

    private StatsServiceImpl service;

    private final LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
    private final LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

    @Test
    void saveHit_shouldMapAndPersist() {
        service = new StatsServiceImpl(repository, mapper);
        EndpointHitDto input = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .timestamp("2024-05-01 12:00:00")
                .build();
        EndpointHit saved = EndpointHit.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .timestamp(LocalDateTime.of(2024, 5, 1, 12, 0, 0))
                .build();
        when(repository.save(any(EndpointHit.class))).thenReturn(saved);

        EndpointHitDto result = service.saveHit(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTimestamp()).isEqualTo("2024-05-01 12:00:00");
    }

    @Test
    void getStats_withoutUris_notUnique_shouldCallFindAllHits() {
        service = new StatsServiceImpl(repository, mapper);
        List<ViewStatsDto> expected = List.of(new ViewStatsDto("app", "/events/1", 5L));
        when(repository.findAllHits(start, end)).thenReturn(expected);

        List<ViewStatsDto> result = service.getStats(start, end, null, false);

        assertThat(result).isEqualTo(expected);
        verify(repository).findAllHits(start, end);
    }

    @Test
    void getStats_withoutUris_unique_shouldCallFindAllUniqueHits() {
        service = new StatsServiceImpl(repository, mapper);
        when(repository.findAllUniqueHits(start, end)).thenReturn(List.of());

        service.getStats(start, end, List.of(), true);

        verify(repository).findAllUniqueHits(start, end);
    }

    @Test
    void getStats_withUris_notUnique_shouldCallFindHitsByUris() {
        service = new StatsServiceImpl(repository, mapper);
        List<String> uris = List.of("/events/1", "/events/2");
        when(repository.findHitsByUris(start, end, uris)).thenReturn(List.of());

        service.getStats(start, end, uris, false);

        verify(repository).findHitsByUris(start, end, uris);
    }

    @Test
    void getStats_withUris_unique_shouldCallFindUniqueHitsByUris() {
        service = new StatsServiceImpl(repository, mapper);
        List<String> uris = List.of("/events/1");
        when(repository.findUniqueHitsByUris(start, end, uris)).thenReturn(List.of());

        service.getStats(start, end, uris, true);

        verify(repository).findUniqueHitsByUris(start, end, uris);
    }

    @Test
    void getStats_startAfterEnd_shouldThrowValidationException() {
        service = new StatsServiceImpl(repository, mapper);

        assertThatThrownBy(() -> service.getStats(end, start, null, false))
                .isInstanceOf(ValidationException.class);
    }
}
