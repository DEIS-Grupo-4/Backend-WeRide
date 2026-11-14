package org.example.backendweride.platform.booking.domain.services;

import org.example.backendweride.platform.booking.domain.model.queries.*;
import org.example.backendweride.platform.booking.interfaces.resources.BookingResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service interface for handling booking-related queries.
 *
 * @summary This service manages read operations for bookings,
 *          including retrieving bookings by ID, customer, date range, and search criteria.
 */
public interface BookingQueryService {

    /**
     * Retrieves a booking by its identifier.
     *
     * @param query query containing the booking identifier
     * @return optional booking resource
     */
    Optional<BookingResource> getBookingById(GetBookingByIdQuery query);

    /**
     * Retrieves the bookings associated with a customer.
     *
     * @param query query containing the customer information
     * @param pageable pagination and sorting information
     * @return page of customer bookings
     */
    Page<BookingResource> getBookingsByCustomer(GetBookingsByCustomerQuery query, Pageable pageable);

    /**
     * Retrieves draft bookings associated with a customer.
     *
     * @param query query containing the customer information
     * @param pageable pagination and sorting information
     * @return page of customer booking drafts
     */
    Page<BookingResource> getBookingDraftsByCustomer(GetBookingDraftsByCustomerQuery query, Pageable pageable);

    /**
     * Retrieves bookings within a specified date range.
     *
     * @param query query containing the date range
     * @param pageable pagination and sorting information
     * @return page of bookings within the date range
     */
    Page<BookingResource> getBookingsByDateRange(GetBookingsByDateRangeQuery query, Pageable pageable);

    /**
     * Searches bookings using the supplied search criteria.
     *
     * @param query booking search criteria
     * @param pageable pagination and sorting information
     * @return page of matching bookings
     */
    Page<BookingResource> searchBookings(SearchBookingsQuery query, Pageable pageable);

    /**
     * Retrieves all bookings using pagination.
     *
     * @param query query used to request all bookings
     * @param pageable pagination and sorting information
     * @return page of bookings
     */
    Page<BookingResource> getAllBookings(GetAllBookingsQuery query, Pageable pageable);

    /**
     * Retrieves bookings associated with a vehicle.
     *
     * @param query query containing the vehicle identifier
     * @param pageable pagination and sorting information
     * @return page of vehicle bookings
     */
    Page<BookingResource> getBookingsByVehicle(GetBookingsByVehicleQuery query, Pageable pageable);

    /**
     * Retrieves bookings matching the specified status.
     *
     * @param query query containing the booking status
     * @param pageable pagination and sorting information
     * @return page of bookings matching the status
     */
    Page<BookingResource> getBookingsByStatus(GetBookingsByStatusQuery query, Pageable pageable);

    /**
     * Retrieves bookings belonging to a user.
     *
     * @param query query containing the user identifier
     * @param pageable pagination and sorting information
     * @return page of user bookings
     */
    Page<BookingResource> getBookingsByUserId(GetBookingsByUserIdQuery query, Pageable pageable);

    /**
     * Retrieves pending bookings belonging to a user.
     *
     * @param query query containing the user identifier
     * @param pageable pagination and sorting information
     * @return page of pending bookings
     */
    Page<BookingResource> getPendingBookingsByUser(GetPendingBookingsByUserQuery query, Pageable pageable);

    /**
     * Retrieves completed bookings belonging to a user.
     *
     * @param query query containing the user identifier
     * @param pageable pagination and sorting information
     * @return page of completed bookings
     */
    Page<BookingResource> getCompletedBookingsByUser(GetCompletedBookingsByUserQuery query, Pageable pageable);
}
