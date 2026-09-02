package ru.practicum.location.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.location.dto.NamedLocationDto;
import ru.practicum.location.service.LocationService;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
public class PublicLocationController {

    private final LocationService service;

    @GetMapping
    public List<NamedLocationDto> getLocations(@RequestParam(defaultValue = "0") int from,
                                           @RequestParam(defaultValue = "10") int size) {
        return service.getLocations(from, size);
    }

    @GetMapping("/{locationId}")
    public NamedLocationDto getLocation(@PathVariable Long locationId) {
        return service.getLocation(locationId);
    }

    @GetMapping("/{locationId}/events")
    public List<EventShortDto> getEventsInLocation(@PathVariable Long locationId,
                                                    @RequestParam(defaultValue = "0") int from,
                                                    @RequestParam(defaultValue = "10") int size) {
        return service.getEventsInLocation(locationId, from, size);
    }
}
