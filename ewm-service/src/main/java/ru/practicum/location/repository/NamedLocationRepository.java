package ru.practicum.location.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.location.model.NamedLocation;

public interface NamedLocationRepository extends JpaRepository<NamedLocation, Long> {
}
