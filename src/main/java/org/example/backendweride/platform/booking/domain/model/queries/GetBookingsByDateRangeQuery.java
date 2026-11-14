package org.example.backendweride.platform.booking.domain.model.queries;

import java.time.LocalDate;

/**
 * Query to get bookings within a specific date range with pagination.
 *
 * @summary This query retrieves bookings that fall within the specified date range,
 *          supporting pagination through page and size parameters.
 */
public record GetBookingsByDateRangeQuery(
    LocalDate from,
    LocalDate to,
    int page,
    int size
) {
}
