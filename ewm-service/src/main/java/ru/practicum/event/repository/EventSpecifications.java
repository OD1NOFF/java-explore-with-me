package ru.practicum.event.repository;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;

/**
 * Спецификации фильтрации событий. Позволяют собирать один SQL-запрос
 * из произвольной комбинации опциональных фильтров без ветвлений и N+1.
 */
public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> hasState(EventState state) {
        return (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    public static Specification<Event> hasStates(List<EventState> states) {
        return (root, query, cb) -> states == null || states.isEmpty() ? null : root.get("state").in(states);
    }

    public static Specification<Event> hasInitiators(List<Long> userIds) {
        return (root, query, cb) ->
                userIds == null || userIds.isEmpty() ? null : root.get("initiator").get("id").in(userIds);
    }

    public static Specification<Event> hasCategories(List<Long> categoryIds) {
        return (root, query, cb) ->
                categoryIds == null || categoryIds.isEmpty() ? null : root.get("category").get("id").in(categoryIds);
    }

    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, cb) -> paid == null ? null : cb.equal(root.get("paid"), paid);
    }

    public static Specification<Event> eventDateAfter(LocalDateTime start) {
        return (root, query, cb) -> start == null ? null : cb.greaterThanOrEqualTo(root.get("eventDate"), start);
    }

    public static Specification<Event> eventDateBefore(LocalDateTime end) {
        return (root, query, cb) -> end == null ? null : cb.lessThanOrEqualTo(root.get("eventDate"), end);
    }

    public static Specification<Event> textContains(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String pattern = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("annotation")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    /**
     * Событие доступно: лимит участников не задан, либо число подтверждённых
     * заявок ещё не достигло лимита. Реализовано коррелированным подзапросом,
     * чтобы не тянуть заявки в приложение и не делать по запросу на событие.
     */
    public static Specification<Event> onlyAvailable() {
        return (root, query, cb) -> {
            Subquery<Long> confirmedCount = query.subquery(Long.class);
            var requestRoot = confirmedCount.from(Request.class);
            confirmedCount.select(cb.count(requestRoot))
                    .where(
                            cb.equal(requestRoot.get("event").get("id"), root.get("id")),
                            cb.equal(requestRoot.get("status"), RequestStatus.CONFIRMED)
                    );
            Predicate noLimit = cb.equal(root.get("participantLimit"), 0);
            Predicate underLimit = cb.lessThan(confirmedCount, cb.toLong(root.get("participantLimit")));
            return cb.or(noLimit, underLimit);
        };
    }
}
