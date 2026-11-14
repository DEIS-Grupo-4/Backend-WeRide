package org.example.backendweride.platform.booking.application.queryservice;

import org.example.backendweride.platform.booking.domain.model.queries.SearchBookingsQuery;
import org.example.backendweride.platform.booking.domain.model.queries.GetPendingBookingsByUserQuery;
import org.example.backendweride.platform.booking.domain.model.aggregates.Booking;
import org.example.backendweride.platform.booking.infraestructure.persistence.jpa.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingQueryServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingQueryServiceImpl service;
    private final Pageable pageable = PageRequest.of(0, 20);
    private final Page<Booking> emptyPage = new PageImpl<>(List.of());

    @Test
    void searchBookings_withoutCustomerId_returnsEmptyPage() {

        // Arrange
        var query = new SearchBookingsQuery(null, 5L, "confirmed", null, null, 0, 20);

        // Act
        var result = service.searchBookings(query, pageable);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void searchBookings_withCustomerIdAndStatus_filtersByBoth() {

        // Arrange
        when(
                bookingRepository.findByUserIdAndStatus(
                        eq(42L),
                        eq("confirmed"),
                        any(Pageable.class)
                )
        ).thenReturn(emptyPage);

        // Act
        var result = service.searchBookings(
                new SearchBookingsQuery(
                        42L,
                        null,
                        "confirmed",
                        null,
                        null,
                        0,
                        20
                ),
                pageable
        );

        // Assert
        assertTrue(result.isEmpty());
        verify(bookingRepository).findByUserIdAndStatus(42L, "confirmed", pageable);
    }

    @Test
    void searchBookings_withCustomerIdOnly_filtersByUser() {

        // Arrange
        when(bookingRepository.findByUserId(eq(42L), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        var result = service.searchBookings(new SearchBookingsQuery(42L, null, null, null, null, 0, 20), pageable);

        // Assert
        assertTrue(result.isEmpty());
        verify(bookingRepository).findByUserId(42L, pageable);
    }

    @Test
    void searchBookings_withCustomerIdAndVehicleId_filtersByBoth() {

        // Arrange
        when(bookingRepository.findByUserIdAndVehicleId(eq(42L), eq(5L), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        var result = service.searchBookings(new SearchBookingsQuery(42L, 5L, null, null, null, 0, 20), pageable);

        // Assert
        assertTrue(result.isEmpty());
        verify(bookingRepository).findByUserIdAndVehicleId(42L, 5L, pageable);
    }

    @Test
    void searchBookings_withCustomerIdAndDateRange_filtersByBoth() {

        // Arrange
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(23, 59, 59);

        when(
                bookingRepository.findByUserIdAndStartDateBetween(
                        eq(42L),
                        eq(fromDateTime),
                        eq(toDateTime),
                        any(Pageable.class)
                )
        ).thenReturn(emptyPage);

        // Act
        var result = service.searchBookings(new SearchBookingsQuery(42L, null, null, from, to, 0, 20), pageable);

        // Assert
        assertTrue(result.isEmpty());

        verify(bookingRepository).findByUserIdAndStartDateBetween(42L, fromDateTime, toDateTime, pageable);
    }

    @Test
    void getPendingBookingsByUser_combinesPendingAndConfirmedBookings() {

        // Arrange
        var userId = 7L;
        var pendingBooking = bookingWithStatus(1L, userId, "pending");
        var confirmedBooking = bookingWithStatus(2L, userId, "confirmed");
        var pendingPageable = PageRequest.of(0, 5);

        when(
                bookingRepository.findByUserIdAndStatus(
                        eq(userId),
                        eq("pending"),
                        any(Pageable.class)
                )
        ).thenReturn(new PageImpl<>(List.of(pendingBooking)));

        when(
                bookingRepository.findByUserIdAndStatus(
                        eq(userId),
                        eq("confirmed"),
                        any(Pageable.class)
                )
        ).thenReturn(new PageImpl<>(List.of(confirmedBooking)));

        // Act
        var result = service.getPendingBookingsByUser(new GetPendingBookingsByUserQuery(userId), PageRequest.of(0, 10));

        // Assert
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals("pending", result.getContent().get(0).status());
        assertEquals("confirmed", result.getContent().get(1).status());

        verify(bookingRepository).findByUserIdAndStatus(userId, "pending", pendingPageable);
        verify(bookingRepository).findByUserIdAndStatus(userId, "confirmed", pendingPageable);
    }

    @Test
    void getPendingBookingsByUser_splitsPageSizeBetweenTheTwoStatuses() {

        // Arrange
        var userId = 7L;
        var requestedPageable = PageRequest.of(0, 10);

        when(
                bookingRepository.findByUserIdAndStatus(
                        eq(userId),
                        eq("pending"),
                        any(Pageable.class)
                )
        ).thenReturn(new PageImpl<>(List.of()));

        when(
                bookingRepository.findByUserIdAndStatus(
                        eq(userId),
                        eq("confirmed"),
                        any(Pageable.class)
                )
        ).thenReturn(new PageImpl<>(List.of()));

        // Act
        service.getPendingBookingsByUser(new GetPendingBookingsByUserQuery(userId), requestedPageable);

        // Assert
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(bookingRepository, times(2)).findByUserIdAndStatus(
                eq(userId),
                statusCaptor.capture(),
                pageableCaptor.capture()
        );

        assertEquals(List.of("pending", "confirmed"), statusCaptor.getAllValues());

        for (Pageable used : pageableCaptor.getAllValues()) {
            assertEquals(5, used.getPageSize());assertEquals(requestedPageable.getPageNumber(), used.getPageNumber());
        }
    }

    private Booking bookingWithStatus(Long bookingId, Long userId, String status) {
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setStatus(status);
        booking.setBookingId(bookingId);

        return booking;
    }
}
