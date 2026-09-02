package ru.practicum.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ApiError(List<String> errors, String message, String reason, String status, String timestamp) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ApiError of(String status, String reason, String message) {
        return new ApiError(List.of(), message, reason, status, LocalDateTime.now().format(FORMATTER));
    }
}
