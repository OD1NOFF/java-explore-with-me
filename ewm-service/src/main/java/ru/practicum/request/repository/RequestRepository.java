package ru.practicum.request.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;

public interface RequestRepository extends JpaRepository<Request, Long> {

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<Request> findAllByRequesterId(Long requesterId);

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByEventIdAndIdIn(Long eventId, List<Long> ids);

    List<Request> findAllByEventIdAndStatus(Long eventId, RequestStatus status);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("select r.event.id as eventId, count(r) as count "
            + "from Request r "
            + "where r.event.id in :eventIds and r.status = ru.practicum.request.model.RequestStatus.CONFIRMED "
            + "group by r.event.id")
    List<EventConfirmedCount> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);
}
