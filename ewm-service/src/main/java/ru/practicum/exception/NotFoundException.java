package ru.practicum.exception;

/**
 * Запрошенный объект не найден или недоступен (маппится в 404).
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
