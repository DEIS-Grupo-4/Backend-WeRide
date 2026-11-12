package org.example.backendweride.platform.trip.application.internal.commands;

import org.example.backendweride.platform.notifications.interfaces.acl.NotificationContextFacade;
import org.example.backendweride.platform.trip.domain.aggregates.Trip;
import org.example.backendweride.platform.trip.infrastructure.persistence.jpa.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripCommandServiceImplTest {

    @Mock private TripRepository tripRepository;

    @Mock private NotificationContextFacade notificationContextFacade;

    @InjectMocks private TripCommandServiceImpl service;

    @Test
    void handle_returnsFalse_whenTripBelongsToAnotherUser() {

        // Arrange
        Trip foreign = mock(Trip.class);
        when(foreign.getUserId()).thenReturn("999");
        when(tripRepository.findById(7L)).thenReturn(Optional.of(foreign));

        // Act
        boolean result = service.handle(7L, "42");

        // Assert
        assertFalse(result, "Service should return false when trying to delete a foreign trip");
        verify(tripRepository).findById(7L);
        verify(tripRepository, never()).delete(any(Trip.class));
    }

    @Test
    void handle_returnsTrue_whenOwnedByCurrentUser() {

        // Arrange
        Trip trip = mock(Trip.class);
        when(trip.getUserId()).thenReturn("42");
        when(tripRepository.findById(7L)).thenReturn(Optional.of(trip));

        // Act
        boolean result = service.handle(7L, "42");

        // Assert
        assertTrue(result, "Service should return true after successful deletion");
        verify(tripRepository).findById(7L);
        verify(tripRepository).delete(trip);
    }
}
