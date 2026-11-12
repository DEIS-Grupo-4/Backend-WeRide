package org.example.backendweride.platform.booking.application.commandservice;

import org.example.backendweride.platform.booking.domain.model.aggregates.Booking;
import org.example.backendweride.platform.booking.domain.model.commands.SaveBookingDraftCommand;
import org.example.backendweride.platform.booking.domain.services.BookingDraftValidationService;
import org.example.backendweride.platform.booking.infraestructure.persistence.jpa.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingDraftServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingDraftValidationService validationService;

    @InjectMocks
    private BookingDraftServiceImpl service;

    private SaveBookingDraftCommand draftCommand(LocalDateTime startDate) { return new SaveBookingDraftCommand(1L, 2L, 3L, 4L, LocalDateTime.now(), startDate, startDate.plusMinutes(30), null, null, "draft", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "card", "pending", 0.0, 30, 0.0, null, null); }

    @Test
    void saveDraft_returnsFailure_whenValidationFails() {

        // Arrange
        var pastCommand = draftCommand(LocalDateTime.now().minusDays(1));

        when(validationService.validateForSave(pastCommand)).thenReturn(new BookingDraftValidationService.ValidationResult(false, "Start date cannot be in the past"));

        // Act
        var result = service.saveDraft(pastCommand);

        // Assert
        assertFalse(result.success());
        assertEquals("Start date cannot be in the past", result.message());
        assertNull(result.draftId());

        verify(validationService).validateForSave(pastCommand);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void saveDraft_savesDraft_whenValidationPasses() {

        // Arrange
        var command = draftCommand(LocalDateTime.now().plusDays(1));

        when(validationService.validateForSave(command)).thenReturn(new BookingDraftValidationService.ValidationResult(true, "Validation successful"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);

        // Act
        var result = service.saveDraft(command);

        // Assert
        assertTrue(result.success());
        assertEquals("Draft saved successfully", result.message());

        verify(validationService).validateForSave(command);
        verify(bookingRepository).save(captor.capture());

        Booking savedBooking = captor.getValue();

        assertNotNull(savedBooking.getBookingId());
        assertEquals(command.userId(), savedBooking.getUserId());
        assertEquals(command.vehicleId(), savedBooking.getVehicleId());
        assertEquals("draft", savedBooking.getStatus());
        assertEquals("pending", savedBooking.getPaymentStatus());
        assertEquals(BigDecimal.ZERO, savedBooking.getTotalCost());
        assertEquals(BigDecimal.ZERO, savedBooking.getFinalCost());
        assertEquals(savedBooking.getBookingId(), result.draftId());
    }
}
