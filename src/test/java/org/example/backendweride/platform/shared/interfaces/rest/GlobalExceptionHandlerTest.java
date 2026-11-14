package org.example.backendweride.platform.shared.interfaces.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private void validationTarget(String brand) { }

    @Test
    void mapsIllegalArgumentExceptionToNotFound() {
        var response = handler.handleIllegalArgument(new IllegalArgumentException("Account not found"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Account not found", response.getBody());
    }

    @Test
    void mapsDuplicateRuntimeExceptionToConflict() {
        var response = handler.handleRuntimeException(new RuntimeException("User already exists"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void mapsNotFoundRuntimeExceptionToNotFound() {
        var response = handler.handleRuntimeException(new RuntimeException("User not found"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void mapsInvalidCredentialsRuntimeExceptionToUnauthorized() {

        // Arrange
        var exception = new RuntimeException("Invalid credentials");

        // Act
        var response = handler.handleRuntimeException(exception);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody());
    }

    @Test
    void rethrowsUnrecognizedRuntimeException() {
        assertThrows(
                RuntimeException.class,
                () -> handler.handleRuntimeException(
                        new RuntimeException("Something unexpected")
                )
        );
    }

    @Test
    void mapsValidationExceptionToBadRequestWithFieldErrors() throws NoSuchMethodException {

        // Arrange
        var bindingResult = new MapBindingResult(new HashMap<>(), "resource");

        bindingResult.addError(new FieldError("resource", "brand", "must not be blank"));

        MethodParameter param = new MethodParameter(
                this.getClass().getDeclaredMethod("validationTarget", String.class),
                0
        );

        var exception = new MethodArgumentNotValidException(param, bindingResult);

        // Act
        var response = handler.handleValidation(exception);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("brand: must not be blank", response.getBody());
    }
}
