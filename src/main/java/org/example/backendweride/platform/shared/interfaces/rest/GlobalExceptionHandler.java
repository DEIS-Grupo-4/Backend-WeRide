package org.example.backendweride.platform.shared.interfaces.rest;

import org.example.backendweride.platform.shared.domain.model.exceptions.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Mapea a códigos HTTP concretos las excepciones de dominio que el proyecto ya lanza
 * (no introduce tipos de excepción nuevos, ver Fase 4 design spec — P-10 mínimo viable).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles conflicts raised by application operations.
     *
     * @param ex exception containing conflict details
     * @return response with HTTP 409 and the exception message
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<String> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    /**
     * Handles database integrity constraint violations.
     *
     * @param ex exception containing database integrity details
     * @return response describing the integrity violation
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // Race fallback: two identical booking requests can slip past the overlap check
        // and hit the uk_booking_vehicle_start constraint. Map to 409.
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (message.contains("uk_booking_vehicle_start") || message.contains("booking_vehicle_start")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Vehicle is already booked for the requested time range");
        }
        throw ex;
    }

    /**
     * Handles illegal argument exceptions.
     *
     * @param ex exception containing the invalid argument details
     * @return response with the corresponding error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Handles runtime exceptions not processed by more specific handlers.
     *
     * @param ex runtime exception raised during request processing
     * @return response containing the exception message
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (message.contains("already exists")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
        if (message.contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
        if (message.contains("invalid credentials")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        }
        throw ex;
    }

    /**
     * Handles request validation errors.
     *
     * @param ex exception containing the validation errors
     * @return response describing the validation failure
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detail);
    }
}
