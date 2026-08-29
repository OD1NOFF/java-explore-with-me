package ru.practicum.client;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

@Slf4j
@Component
public class StatsClient {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;

    public StatsClient(@Value("${stats-server.url}") String serverUrl, RestTemplateBuilder builder) {
        this.restTemplate = builder.rootUri(serverUrl).build();
    }

    public void saveHit(String app, String uri, String ip, LocalDateTime timestamp) {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp.format(TIMESTAMP_FORMATTER))
                .build();
        try {
            restTemplate.postForEntity("/hit", hitDto, Void.class);
        } catch (Exception e) {
            log.warn("Failed to send hit to stats service: {}", e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        Map<String, Object> params = new HashMap<>();
        params.put("start", start.format(TIMESTAMP_FORMATTER));
        params.put("end", end.format(TIMESTAMP_FORMATTER));
        params.put("unique", unique);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", "{start}")
                .queryParam("end", "{end}")
                .queryParam("unique", "{unique}");

        if (uris != null && !uris.isEmpty()) {
            uriBuilder.queryParam("uris", "{uris}");
            params.put("uris", String.join(",", uris));
        }

        URI uri = uriBuilder.encode().buildAndExpand(params).toUri();
        ViewStatsDto[] result = restTemplate.getForObject(uri, ViewStatsDto[].class);
        return result == null ? List.of() : Arrays.asList(result);
    }
}
