package org.example.backendweride.platform.booking.application.commandservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.backendweride.platform.booking.domain.services.BookingCommandService;
import org.example.backendweride.platform.booking.domain.model.commands.SaveBookingDraftCommand;
import org.example.backendweride.platform.booking.domain.model.commands.CreateBookingCommand;
import org.example.backendweride.platform.booking.domain.model.commands.UpdateBookingCommand;
import org.example.backendweride.platform.booking.domain.model.exceptions.BookingConflictException;
import org.example.backendweride.platform.booking.infraestructure.persistence.jpa.BookingRepository;
import org.example.backendweride.platform.garage.infrastructure.persistence.jpa.VehicleRepository;
import org.example.backendweride.platform.location.infrastructure.persistence.jpa.LocationRepository;
import org.example.backendweride.platform.booking.domain.model.aggregates.Booking;
import org.example.backendweride.platform.notifications.interfaces.acl.NotificationContextFacade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of BookingCommandService to handle booking-related commands.
 *
 * @summary This service processes commands for saving booking drafts and creating bookings.
 *          Validation, server-side cost computation and conflict detection live here.
 */
@Service
public class BookingCommandServiceImpl implements BookingCommandService {

    private static final List<String> ACTIVE_BOOKING_STATUSES =
            List.of("confirmed", "in_progress", "pending");

    private final BookingRepository bookingRepository;

    private final VehicleRepository vehicleRepository;

    private final LocationRepository locationRepository;

    private final NotificationContextFacade notificationContextFacade;

    public BookingCommandServiceImpl(BookingRepository bookingRepository,
                                     VehicleRepository vehicleRepository,
                                     LocationRepository locationRepository,
                                     NotificationContextFacade notificationContextFacade) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.locationRepository = locationRepository;
        this.notificationContextFacade = notificationContextFacade;
    }

    @Override
    @Transactional
    public SaveDraftResult saveDraft(SaveBookingDraftCommand command) {
        var validationMessage = validateSchedule(command.startDate(), command.endDate(), command.duration());
        if (validationMessage != null) {
            return new SaveDraftResult(null, false, validationMessage);
        }

        var vehicleOpt = vehicleRepository.findById(command.vehicleId());
        if (vehicleOpt.isEmpty()) {
            return new SaveDraftResult(null, false, "Vehicle not found");
        }

        if (command.startLocationId() != null && command.endLocationId() != null
                && (!locationRepository.existsById(command.startLocationId())
                || !locationRepository.existsById(command.endLocationId()))) {
            return new SaveDraftResult(null, false, "Location not found");
        }

        Booking booking = Booking.createDraftFrom(command);
        booking.calculateCost(BigDecimal.valueOf(vehicleOpt.get().getPricePerMinute()));

        Booking saved = bookingRepository.save(booking);

        return new SaveDraftResult(saved.getBookingId(), true, "Draft saved");
    }

    @Override
    @Transactional
    public CreateBookingResult createBooking(CreateBookingCommand command) {
        // Server-side validation of every trust boundary
        if (command.userId() == null) {
            return new CreateBookingResult(null, false, "User ID is required");
        }
        if (command.vehicleId() == null) {
            return new CreateBookingResult(null, false, "Vehicle ID is required");
        }
        if (command.startLocationId() == null || command.endLocationId() == null) {
            return new CreateBookingResult(null, false, "Start and end locations are required");
        }
        if (command.paymentMethod() == null || command.paymentMethod().isBlank()) {
            return new CreateBookingResult(null, false, "Payment method is required");
        }

        var validationMessage = validateSchedule(command.startDate(), command.endDate(), command.duration());
        if (validationMessage != null) {
            return new CreateBookingResult(null, false, validationMessage);
        }

        var vehicleOpt = vehicleRepository.findById(command.vehicleId());
        if (vehicleOpt.isEmpty()) {
            return new CreateBookingResult(null, false, "Vehicle not found");
        }

        var vehicle = vehicleOpt.get();
        if (!isVehicleAvailable(vehicle)) {
            return new CreateBookingResult(null, false, "Vehicle is not available");
        }

        if (!locationRepository.existsById(command.startLocationId())
                || !locationRepository.existsById(command.endLocationId())) {
            return new CreateBookingResult(null, false, "Location not found");
        }

        ensureNoOverlap(command.vehicleId(), command.startDate(), endOf(command), null);

        // Server-side pricing: ignore any client-provided cost fields
        Booking booking = Booking.createConfirmedFrom(command);
        booking.calculateCost(BigDecimal.valueOf(vehicle.getPricePerMinute()));
        booking.setStatus("confirmed");
        booking.setPaymentStatus("pending");

        Booking saved = bookingRepository.save(booking);

        notificationContextFacade.notifyAccount(
                command.userId(),
                "Reserva confirmada",
                "Tu reserva del vehículo fue confirmada.",
                "booking",
                "confirmation",
                String.valueOf(saved.getBookingId()),
                "booking");

        return new CreateBookingResult(saved.getBookingId(), true, "Booking created");
    }

    @Override
    @Transactional
    public Optional<Booking> updateBooking(Long id, Long userId, UpdateBookingCommand command) {
        return findByIdentifier(id)
                .filter(booking -> userId.equals(booking.getUserId()))
                .map(booking -> {
                    if (command.startDate() != null || command.endDate() != null || command.duration() != null) {
                        var newStart = command.startDate() != null ? command.startDate() : booking.getStartDate();
                        var newEnd = command.endDate() != null ? command.endDate() : booking.getEndDate();
                        var newDuration = command.duration() != null ? command.duration() : booking.getDuration();

                        var validationMessage = validateSchedule(newStart, newEnd, newDuration);
                        if (validationMessage != null) {
                            throw new BookingConflictException(validationMessage);
                        }

                        ensureNoOverlap(booking.getVehicleId(), newStart, newEnd, booking.getBookingId());
                    }

                    // Server-side: recompute cost when duration changes
                    var recalc = command.duration() != null;
                    booking.updateFrom(command);
                    if (recalc && booking.getVehicleId() != null) {
                        vehicleRepository.findById(booking.getVehicleId())
                                .ifPresent(v -> booking.calculateCost(BigDecimal.valueOf(v.getPricePerMinute())));
                    }
                    return bookingRepository.save(booking);
                });
    }

    @Override
    @Transactional
    public boolean deleteBooking(Long id, Long userId) {
        var booking = findByIdentifier(id).filter(item -> userId.equals(item.getUserId()));
        booking.ifPresent(bookingRepository::delete);
        return booking.isPresent();
    }

    /**
     * @return null when the schedule is valid, otherwise a validation message.
     */
    private String validateSchedule(LocalDateTime startDate, LocalDateTime endDate, Integer duration) {
        if (startDate == null) {
            return "Start date is required";
        }
        if (startDate.isBefore(LocalDateTime.now())) {
            return "Start date cannot be in the past";
        }
        if (duration == null || duration <= 0) {
            return "Duration must be greater than 0";
        }
        if (duration > 1440) {
            return "Duration cannot exceed 24 hours";
        }
        return null;
    }

    private boolean isVehicleAvailable(
            org.example.backendweride.platform.garage.domain.model.aggregates.Vehicle vehicle
    ) {
        return vehicle.getStatus() == null || vehicle.getStatus().equalsIgnoreCase("available");
    }

    private LocalDateTime endOf(CreateBookingCommand command) {
        return command.endDate() != null
                ? command.endDate()
                : command.startDate().plusMinutes(command.duration());
    }

    private void ensureNoOverlap(Long vehicleId, LocalDateTime start, LocalDateTime end, Long excludeBookingId) {
        if (start == null || end == null || end.isBefore(start)) {
            return;
        }
        boolean overlaps = bookingRepository.existsOverlappingBooking(
                vehicleId, start, end, ACTIVE_BOOKING_STATUSES, excludeBookingId);
        if (overlaps) {
            throw new BookingConflictException(
                    "Vehicle is already booked for the requested time range");
        }
    }

    private Optional<Booking> findByIdentifier(Long id) {
        return bookingRepository.findById(id).or(() -> bookingRepository.findByBookingId(id));
    }
}
