package ru.practicum.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Пагинация по смещению (from/size из спецификации), а не по номеру страницы,
 * как того требует стандартный Spring Data Pageable.
 */
public class OffsetPageRequest implements Pageable {

    private final int offset;
    private final int limit;
    private final Sort sort;

    private OffsetPageRequest(int offset, int limit, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("from must not be negative");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
        this.offset = offset;
        this.limit = limit;
        this.sort = sort;
    }

    public static OffsetPageRequest of(int from, int size) {
        return new OffsetPageRequest(from, size, Sort.unsorted());
    }

    public static OffsetPageRequest of(int from, int size, Sort sort) {
        return new OffsetPageRequest(from, size, sort);
    }

    @Override
    public int getPageNumber() {
        return offset / limit;
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(offset + limit, limit, sort);
    }

    public Pageable previous() {
        return offset - limit < 0 ? first() : new OffsetPageRequest(offset - limit, limit, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, limit, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest(pageNumber * limit, limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
