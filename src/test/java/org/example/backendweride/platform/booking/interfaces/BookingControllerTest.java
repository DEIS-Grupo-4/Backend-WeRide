package org.example.backendweride.platform.booking.interfaces;

import org.example.backendweride.platform.booking.domain.model.commands.DeleteBookingDraftCommand;
import org.example.backendweride.platform.booking.domain.model.queries.*;
import org.example.backendweride.platform.booking.domain.services.BookingCommandService;
import org.example.backendweride.platform.booking.domain.services.BookingDraftService;
import org.example.backendweride.platform.booking.domain.services.BookingQueryService;
import org.example.backendweride.platform.booking.interfaces.resources.BookingResource;
import org.example.backendweride.platform.iam.application.internal.outboundservices.security.AuthenticatedAccountProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingCommandService commandService;

    @Mock
    private BookingQueryService bookingQueryService;

    @Mock
    private BookingDraftService draftService;

    @Mock
    private AuthenticatedAccountProvider authenticatedAccountProvider;

    @InjectMocks
    private BookingController controller;
    private static final Page<BookingResource> EMPTY_PAGE = new PageImpl<>(List.of());

    @Test
    void getBookingsByUserId_usesAuthenticatedUserId_notPathVariable() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(
                bookingQueryService.getBookingsByUserId(
                        any(GetBookingsByUserIdQuery.class),
                        any(Pageable.class)
                )
        ).thenReturn(EMPTY_PAGE);

        // Act
        var response = controller.getBookingsByUserId(999L, 0, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        ArgumentCaptor<GetBookingsByUserIdQuery> captor = ArgumentCaptor.forClass(GetBookingsByUserIdQuery.class);

        verify(bookingQueryService).getBookingsByUserId(captor.capture(), any(Pageable.class));

        assertEquals(42L, captor.getValue().userId());
    }

    @Test
    void getPendingBookingsByUser_usesAuthenticatedUserId_notPathVariable() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(
                bookingQueryService.getPendingBookingsByUser(
                        any(GetPendingBookingsByUserQuery.class),
                        any(Pageable.class)
                )
        ).thenReturn(EMPTY_PAGE);

        // Act
        var response = controller.getPendingBookingsByUser(999L, 0, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        ArgumentCaptor<GetPendingBookingsByUserQuery> captor =
                ArgumentCaptor.forClass(GetPendingBookingsByUserQuery.class);

        verify(bookingQueryService).getPendingBookingsByUser(captor.capture(), any(Pageable.class));

        assertEquals(42L, captor.getValue().userId());
    }

    @Test
    void getCompletedBookingsByUser_usesAuthenticatedUserId_notPathVariable() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(
                bookingQueryService.getCompletedBookingsByUser(
                        any(GetCompletedBookingsByUserQuery.class),
                        any(Pageable.class)
                )
        ).thenReturn(EMPTY_PAGE);

        // Act
        var response = controller.getCompletedBookingsByUser(999L, 0, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        ArgumentCaptor<GetCompletedBookingsByUserQuery> captor =
                ArgumentCaptor.forClass(GetCompletedBookingsByUserQuery.class);

        verify(bookingQueryService).getCompletedBookingsByUser(captor.capture(), any(Pageable.class));

        assertEquals(42L, captor.getValue().userId());
    }

    @Test
    void getDraftsByCustomer_usesAuthenticatedUserId_notQueryParam() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(
                bookingQueryService.getBookingDraftsByCustomer(
                        any(GetBookingDraftsByCustomerQuery.class),
                        any(Pageable.class)
                )
        ).thenReturn(EMPTY_PAGE);

        // Act
        var response = controller.getDraftsByCustomer(999L, 0, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        ArgumentCaptor<GetBookingDraftsByCustomerQuery> captor =
                ArgumentCaptor.forClass(GetBookingDraftsByCustomerQuery.class);

        verify(bookingQueryService).getBookingDraftsByCustomer(captor.capture(), any(Pageable.class));

        assertEquals(42L, captor.getValue().customerId());
    }

    @Test
    void deleteDraft_usesAuthenticatedUserId_notPathVariable() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(draftService.deleteDraft(any(DeleteBookingDraftCommand.class)))
                .thenReturn(
                        new BookingCommandService.SaveDraftResult(
                                555L,
                                true,
                                "Draft deleted successfully"
                        )
                );

        // Act
        var response = controller.deleteDraft(555L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Draft deleted successfully", response.getBody());

        ArgumentCaptor<DeleteBookingDraftCommand> captor = ArgumentCaptor.forClass(DeleteBookingDraftCommand.class);

        verify(draftService).deleteDraft(captor.capture());

        assertEquals(42L, captor.getValue().userId());
        assertEquals(555L, captor.getValue().draftId());
    }

    private static BookingResource bookingResource(Long id, Long userId) {
        return new BookingResource(
                id,
                id,
                userId,
                1L,
                1L,
                1L,
                null,
                null,
                null,
                null,
                null,
                "confirmed",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    @Test
    void getBookingById_returnsNotFound_whenBookingBelongsToAnotherUser() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(bookingQueryService.getBookingById(any(GetBookingByIdQuery.class)))
                .thenReturn(Optional.of(bookingResource(7L, 999L)));

        // Act
        var response = controller.getBookingById(7L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(bookingQueryService).getBookingById(new GetBookingByIdQuery(7L));
        verify(authenticatedAccountProvider).getCurrentAccountId();
    }

    @Test
    void getBookingById_returnsBooking_whenOwnedByCurrentUser() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(bookingQueryService.getBookingById(any(GetBookingByIdQuery.class)))
                .thenReturn(Optional.of(bookingResource(7L, 42L)));

        // Act
        var response = controller.getBookingById(7L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(42L, response.getBody().userId());
        assertEquals(7L, response.getBody().bookingId());

        verify(bookingQueryService).getBookingById(new GetBookingByIdQuery(7L));
    }

    @Test
    void searchBookings_usesAuthenticatedUserId_andPreservesFilters() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(
                bookingQueryService.searchBookings(
                        any(SearchBookingsQuery.class),
                        any(Pageable.class)
                )
        ).thenReturn(EMPTY_PAGE);

        // Act
        var response = controller.searchBookings(999L, "confirmed", "2026-01-01", "2026-01-31", 0, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        ArgumentCaptor<SearchBookingsQuery> captor = ArgumentCaptor.forClass(SearchBookingsQuery.class);

        verify(bookingQueryService).searchBookings(captor.capture(), any(Pageable.class));

        assertEquals(42L, captor.getValue().customerId());
        assertEquals(999L, captor.getValue().vehicleId());
        assertEquals("confirmed", captor.getValue().status());
    }

    @Test
    void getBookingsByVehicle_scopesToAuthenticatedUser() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(
                bookingQueryService.getBookingsByVehicle(
                        any(GetBookingsByVehicleQuery.class),
                        any(Pageable.class)
                )
        ).thenReturn(EMPTY_PAGE);

        // Act
        var response = controller.getBookingsByVehicle(5L, 0, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<GetBookingsByVehicleQuery> captor = ArgumentCaptor.forClass(GetBookingsByVehicleQuery.class);

        verify(bookingQueryService).getBookingsByVehicle(captor.capture(), any(Pageable.class));

        assertEquals(42L, captor.getValue().userId());
        assertEquals(5L, captor.getValue().vehicleId());
    }

    @Test
    void getBookingsByStatus_scopesToAuthenticatedUser() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(
                bookingQueryService.getBookingsByStatus(
                        any(GetBookingsByStatusQuery.class),
                        any(Pageable.class)
                )
        ).thenReturn(EMPTY_PAGE);

        // Act
        var response = controller.getBookingsByStatus("confirmed", 0, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<GetBookingsByStatusQuery> captor = ArgumentCaptor.forClass(GetBookingsByStatusQuery.class);

        verify(bookingQueryService).getBookingsByStatus(captor.capture(), any(Pageable.class));

        assertEquals(42L, captor.getValue().userId());
        assertEquals("confirmed", captor.getValue().status());
    }
}
