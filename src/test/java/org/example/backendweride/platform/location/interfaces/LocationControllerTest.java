package org.example.backendweride.platform.location.interfaces;

import org.example.backendweride.platform.location.domain.services.commandservices.LocationCommandService;
import org.example.backendweride.platform.location.domain.services.queryservices.LocationQueryService;
import org.example.backendweride.platform.location.interfaces.resources.LocationResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    @Mock
    private LocationCommandService locationCommandService;

    @Mock
    private LocationQueryService locationQueryService;

    @InjectMocks
    private LocationController controller;

    @Test
    void getAllLocation_returnsLocationResources() {

        // Arrange
        LocationResource location = new LocationResource(
                1L,
                "Estación principal",
                "Av. Principal 123",
                null,
                "pickup",
                10,
                4,
                true,
                null,
                List.of("charging"),
                "San Isidro",
                "Estación de prueba",
                "location-image.jpg"
        );
        List<LocationResource> locations = List.of(location);

        when(locationQueryService.handle()).thenReturn(Optional.of(locations));

        // Act
        var response = controller.getAllLocation();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(location, response.getBody().get(0));
        assertEquals("Estación principal", response.getBody().get(0).name());

        verify(locationQueryService).handle();
    }

    @Test
    void getAllLocation_returnsNotFound_whenServiceReturnsEmptyOptional() {

        // Arrange
        when(locationQueryService.handle()).thenReturn(Optional.empty());

        // Act
        var response = controller.getAllLocation();

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(locationQueryService).handle();
    }
}
