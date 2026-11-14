package org.example.backendweride.platform.booking.domain.model.queries;

/**
 * Query to get booking drafts by customer ID with pagination.
 *
 * @summary This query retrieves booking drafts associated with a specific customer,
 *          supporting pagination through page and size parameters.
 */
public record GetBookingDraftsByCustomerQuery(
    Long customerId,
    int page,
    int size
) {
}
