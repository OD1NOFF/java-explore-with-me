package ru.practicum.exception;

/**
 * Операция невозможна из-за текущего состояния данных: нарушение уникальности,
 * несоответствие жизненного цикла события/заявки и т.п. (маппится в 409).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
