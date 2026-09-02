package ru.practicum.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException e) {
        log.warn("Not found: {}", e.getMessage());
        return ApiError.of(HttpStatus.NOT_FOUND.name(), "The required object was not found.", e.getMessage());
    }

    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class,
            ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(Exception e) {
        log.warn("Bad request: {}", e.getMessage());
        return ApiError.of(HttpStatus.BAD_REQUEST.name(), "Incorrectly made request.", e.getMessage());
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(Exception e) {
        log.warn("Conflict: {}", e.getMessage());
        return ApiError.of(HttpStatus.CONFLICT.name(), "Integrity constraint has been violated.", e.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleInternal(Throwable e) {
        log.error("Unexpected error", e);
        return ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR.name(), "Internal error", e.getMessage());
    }
}
