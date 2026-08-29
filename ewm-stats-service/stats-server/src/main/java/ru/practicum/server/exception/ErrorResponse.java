package ru.practicum.server.exception;

public record ErrorResponse(String status, String reason, String message) {
}
