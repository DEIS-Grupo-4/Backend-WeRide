package org.example.backendweride.platform.booking.application.commandservice;

import org.example.backendweride.platform.booking.domain.model.aggregates.Booking;
import org.example.backendweride.platform.booking.domain.model.commands.SaveBookingDraftCommand;
import org.example.backendweride.platform.booking.infraestructure.persistence.jpa.BookingRepository;
import org.example.backendweride.platform.garage.domain.model.aggregates.Vehicle;
import org.example.backendweride.platform.garage.infrastructure.persistence.jpa.VehicleRepository;
import org.example.backendweride.platform.location.infrastructure.persistence.jpa.LocationRepository;
import org.example.backendweride.platform.notifications.interfaces.acl.NotificationContextFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCommandServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private NotificationContextFacade notificationContextFacade;

    @InjectMocks
    private BookingCommandServiceImpl service;

    private SaveBookingDraftCommand draftCommand(Long vehicleId) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.plusDays(1);

        return new SaveBookingDraftCommand(1L, vehicleId, 2L, 3L, now, startDate, startDate.plusMinutes(30), null, null, "draft", null, null, null, "credit_card", "pending", null, 30, null, null, null);
    }

    @Test
    void saveDraft_returnsFailure_whenVehicleDoesNotExist() {

        // Arrange
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        var result = service.saveDraft(draftCommand(999L));

        // Assert
        assertFalse(result.success());
        assertEquals("Vehicle not found", result.message());

        verify(vehicleRepository).findById(999L);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void saveDraft_succeeds_whenVehicleExists() {

        // Arrange
        Vehicle vehicle = mock(Vehicle.class);

        when(vehicle.getPricePerMinute()).thenReturn(0.5);
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(vehicle));
        when(locationRepository.existsById(2L)).thenReturn(true);
        when(locationRepository.existsById(3L)).thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);

        // Act
        var result = service.saveDraft(draftCommand(7L));

        // Assert
        assertTrue(result.success());
        assertEquals("Draft saved", result.message());

        verify(vehicleRepository).findById(7L);
        verify(locationRepository).existsById(2L);
        verify(locationRepository).existsById(3L);
        verify(bookingRepository).save(captor.capture());

        Booking savedBooking = captor.getValue();

        assertEquals("draft", savedBooking.getStatus());
        assertEquals("pending", savedBooking.getPaymentStatus());
        assertEquals(0, savedBooking.getTotalCost().compareTo(new BigDecimal("15.0")));
        assertEquals(0, savedBooking.getFinalCost().compareTo(new BigDecimal("15.0")));
    }
}
