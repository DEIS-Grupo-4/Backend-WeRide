package org.example.backendweride.platform.booking.infraestructure.persistence.jpa;

import org.example.backendweride.platform.booking.domain.model.aggregates.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Booking persistence.
 *
 * @summary This repository provides methods to perform CRUD operations and custom queries on Booking aggregate.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Finds a booking using its business identifier.
     *
     * @param bookingId booking business identifier
     * @return optional matching booking
     */
    Optional<Booking> findByBookingId(Long bookingId);

    /**
     * Finds bookings belonging to a user.
     *
     * @param userId user identifier
     * @param pageable pagination and sorting information
     * @return page of user bookings
     */
    Page<Booking> findByUserId(Long userId, Pageable pageable);

    /**
     * Finds a user's bookings matching a specific status.
     *
     * @param userId user identifier
     * @param status booking status
     * @param pageable pagination and sorting information
     * @return page of matching bookings
     */
    Page<Booking> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    /**
     * Finds bookings matching a specific status.
     *
     * @param status booking status
     * @param pageable pagination and sorting information
     * @return page of matching bookings
     */
    Page<Booking> findByStatus(String status, Pageable pageable);

    /**
     * Finds bookings whose start dates fall within a date range.
     *
     * @param from beginning of the date range
     * @param to end of the date range
     * @param pageable pagination and sorting information
     * @return page of bookings within the date range
     */
    Page<Booking> findByStartDateBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * Finds bookings associated with a vehicle.
     *
     * @param vehicleId vehicle identifier
     * @param pageable pagination and sorting information
     * @return page of vehicle bookings
     */
    Page<Booking> findByVehicleId(Long vehicleId, Pageable pageable);

    /**
     * Finds bookings associated with a user and vehicle.
     *
     * @param userId user identifier
     * @param vehicleId vehicle identifier
     * @param pageable pagination and sorting information
     * @return page of matching bookings
     */
    Page<Booking> findByUserIdAndVehicleId(Long userId, Long vehicleId, Pageable pageable);

    /**
     * Finds a user's bookings within a specified start date range.
     *
     * @param userId user identifier
     * @param from beginning of the date range
     * @param to end of the date range
     * @param pageable pagination and sorting information
     * @return page of matching bookings
     */
    Page<Booking> findByUserIdAndStartDateBetween(Long userId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * Returns true when an active booking already occupies the vehicle for the requested
     * time range. Overlap semantics: existing.start < requested.end AND existing.end > requested.start.
     * endDate is always populated by the aggregate factories (derived from duration when absent).
     *
     * @param vehicleId the vehicle to check
     * @param start     requested start
     * @param end       requested end (must be non-null)
     * @param statuses  the booking statuses that occupy the vehicle
     * @param excludeBookingId booking to ignore (e.g. the booking being updated), or null
     */
    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM Booking b
            WHERE b.vehicleId = :vehicleId
              AND b.status IN :statuses
              AND b.startDate < :end
              AND b.endDate > :start
              AND (:excludeBookingId IS NULL OR b.bookingId <> :excludeBookingId)
            """)
    boolean existsOverlappingBooking(@Param("vehicleId") Long vehicleId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end,
                                     @Param("statuses") List<String> statuses,
                                     @Param("excludeBookingId") Long excludeBookingId);
}
