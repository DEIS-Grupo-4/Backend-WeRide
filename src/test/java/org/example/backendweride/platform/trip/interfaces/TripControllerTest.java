package org.example.backendweride.platform.trip.interfaces;

import org.example.backendweride.platform.iam.application.internal.outboundservices.security.AuthenticatedAccountProvider;
import org.example.backendweride.platform.trip.domain.commands.CreateTripCommand;
import org.example.backendweride.platform.trip.domain.aggregates.Trip;
import org.example.backendweride.platform.trip.domain.services.commands.TripCommandService;
import org.example.backendweride.platform.trip.domain.services.queries.TripQueryService;
import org.example.backendweride.platform.trip.interfaces.resources.CreateTripCommandResource;
import org.example.backendweride.platform.trip.interfaces.resources.TripResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripControllerTest {

    @Mock
    private TripCommandService tripCommandService;

    @Mock
    private TripQueryService tripQueryService;

    @Mock
    private AuthenticatedAccountProvider authenticatedAccountProvider;

    @InjectMocks
    private TripController controller;

    private TripResource sampleTripResource() {
        return new TripResource(1L, 10L, "42", 2L, 3L, 4L, "route", List.of(), new Date(), new Date(), 10, 5f, 20f, 25f, 0f, 0f, 0, "sunny", 20, "active", List.of(), List.of());
    }

    @Test
    void getAllTrips_usesAuthenticatedUserId() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(tripQueryService.handle("42")).thenReturn(Optional.of(List.of()));

        // Act
        var response = controller.getAllTrips();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(authenticatedAccountProvider).getCurrentAccountId();
        verify(tripQueryService).handle("42");
    }

    @Test
    void getAllTrips_returnsTripResources() {

        // Arrange
        TripResource trip = sampleTripResource();

        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(1L);
        when(tripQueryService.handle("1")).thenReturn(Optional.of(List.of(trip)));

        // Act
        var response = controller.getAllTrips();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(trip, response.getBody().get(0));
        assertEquals("42", response.getBody().get(0).userId());

        verify(tripQueryService).handle("1");
    }

    @Test
    void deleteTripById_passesAuthenticatedUserId() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(tripCommandService.handle(7L, "42")).thenReturn(true);

        // Act
        var response = controller.deleteTripById(7L);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(authenticatedAccountProvider).getCurrentAccountId();
        verify(tripCommandService).handle(7L, "42");
    }

    @Test
    void createTrip_usesAuthenticatedUserIdNotResourceBody() {

        // Arrange
        var resource = new CreateTripCommandResource(1L, "someone-else-id", 2L, 3L, 4L, "route", List.of(), new Date(), new Date(), 10, 5f, 20f, 25f, 0f, 0f, 0, "sunny", 20, "active", List.of(), List.of());

        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(tripCommandService.handle(any(CreateTripCommand.class))).thenAnswer(invocation -> Optional.of(new Trip(invocation.getArgument(0))));

        ArgumentCaptor<CreateTripCommand> captor = ArgumentCaptor.forClass(CreateTripCommand.class);

        // Act
        var response = controller.createTrip(resource);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("42", response.getBody().userId());

        verify(authenticatedAccountProvider).getCurrentAccountId();
        verify(tripCommandService).handle(captor.capture());

        assertEquals("42", captor.getValue().userId());
        assertEquals(1L, captor.getValue().bookingId());
        assertEquals(2L, captor.getValue().vehicleId());
    }
}
