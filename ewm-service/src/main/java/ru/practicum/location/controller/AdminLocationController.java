package ru.practicum.location.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.location.dto.NamedLocationDto;
import ru.practicum.location.dto.NewLocationDto;
import ru.practicum.location.dto.UpdateLocationDto;
import ru.practicum.location.service.LocationService;

@RestController
@RequestMapping("/admin/locations")
@RequiredArgsConstructor
public class AdminLocationController {

    private final LocationService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NamedLocationDto addLocation(@RequestBody @Valid NewLocationDto dto) {
        return service.addLocation(dto);
    }

    @PatchMapping("/{locationId}")
    public NamedLocationDto updateLocation(@PathVariable Long locationId, @RequestBody @Valid UpdateLocationDto dto) {
        return service.updateLocation(locationId, dto);
    }

    @DeleteMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable Long locationId) {
        service.deleteLocation(locationId);
    }
}
