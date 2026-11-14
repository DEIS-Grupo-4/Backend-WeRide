package org.example.backendweride.platform.booking.domain.model.queries;

/**
 * Query to get bookings by customer ID with pagination.
 *
 * @summary This query retrieves bookings associated with a specific customer,
 *          supporting pagination through page and size parameters.
 */
public record GetBookingsByCustomerQuery(
    Long customerId,
    int page,
    int size
) {
}
