package org.example.backendweride.platform.booking.interfaces.resources;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

/**
 * BookingResource record representing booking details.
 *
 * @summary This record encapsulates the complete details of a booking including customer
 * and vehicle information, timing, locations, pricing, rating, and status.
 */
public record BookingResource(
    Long id,
    Long bookingId,
    Long userId,
    Long vehicleId,
    Long startLocationId,
    Long endLocationId,
    LocalDateTime reservedAt,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime actualStartDate,
    LocalDateTime actualEndDate,
    String status,
    BigDecimal totalCost,
    BigDecimal discount,
    BigDecimal finalCost,
    String paymentMethod,
    String paymentStatus,
    Double distance,
    Integer duration,
    Double averageSpeed,
    RatingResource rating,
    List<String> issues
) {
    public record RatingResource(Integer score, String comment) {
    }
}
