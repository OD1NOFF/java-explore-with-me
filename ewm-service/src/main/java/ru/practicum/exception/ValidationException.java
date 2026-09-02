package ru.practicum.exception;

/**
 * Запрос составлен некорректно на уровне бизнес-правил (маппится в 400).
 * Для ошибок аннотаций @Valid/@Validated отдельный маппинг в ErrorHandler.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
