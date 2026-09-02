package ru.practicum.event.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.event.model.Event;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    boolean existsByCategoryId(Long categoryId);

    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    List<Event> findAllByIdIn(List<Long> ids);

    @Query(value = "SELECT * FROM events e "
            + "WHERE e.state = 'PUBLISHED' "
            + "AND distance(:lat, :lon, e.lat, e.lon) <= :radius "
            + "ORDER BY e.event_date",
            nativeQuery = true)
    List<Event> findPublishedEventsWithinRadius(@Param("lat") double lat, @Param("lon") double lon,
                                                 @Param("radius") double radius, Pageable pageable);
}
