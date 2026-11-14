package org.example.backendweride.platform.booking.application.queryservice;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import org.example.backendweride.platform.booking.domain.services.BookingQueryService;
import org.example.backendweride.platform.booking.infraestructure.persistence.jpa.BookingRepository;
import org.example.backendweride.platform.booking.interfaces.transform.BookingResourceFromEntityAssembler;
import org.example.backendweride.platform.booking.interfaces.resources.BookingResource;
import org.example.backendweride.platform.booking.domain.model.queries.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service to handle booking-related queries.
 *
 * @summary This service retrieves booking information based on provided queries.
 */
@Service
public class BookingQueryServiceImpl implements BookingQueryService {

    private final BookingRepository bookingRepository;

    public BookingQueryServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public Optional<BookingResource> getBookingById(GetBookingByIdQuery q) {
        if (q == null || q.bookingId() == null) return Optional.empty();
        return bookingRepository.findById(q.bookingId())
                .or(() -> bookingRepository.findByBookingId(q.bookingId()))
                .map(BookingResourceFromEntityAssembler::toResource);
    }

    @Override
    public Page<BookingResource> getBookingsByCustomer(GetBookingsByCustomerQuery q, Pageable pageable) {
        var page = bookingRepository.findByUserId(q.customerId(), pageable);
        return page.map(BookingResourceFromEntityAssembler::toResource);
    }

    @Override
    public Page<BookingResource> getBookingDraftsByCustomer(GetBookingDraftsByCustomerQuery q, Pageable pageable) {
        var page = bookingRepository.findByUserIdAndStatus(q.customerId(), "draft", pageable);
        return page.map(BookingResourceFromEntityAssembler::toResource);
    }

    @Override
    public Page<BookingResource> getBookingsByDateRange(GetBookingsByDateRangeQuery q, Pageable pageable) {
        LocalDateTime from = q.from().atStartOfDay();
        LocalDateTime to = q.to().atTime(23, 59, 59);
        var page = bookingRepository.findByStartDateBetween(from, to, pageable);
        return page.map(BookingResourceFromEntityAssembler::toResource);
    }

    @Override
    public Page<BookingResource> searchBookings(SearchBookingsQuery q, Pageable pageable) {
        if (q == null || q.customerId() == null) return Page.empty(pageable);

        if (q.status() != null) {
            var page = bookingRepository.findByUserIdAndStatus(q.customerId(), q.status(), pageable);
            return page.map(BookingResourceFromEntityAssembler::toResource);
        }

        if (q.vehicleId() != null) {
            var page = bookingRepository.findByUserIdAndVehicleId(q.customerId(), q.vehicleId(), pageable);
            return page.map(BookingResourceFromEntityAssembler::toResource);
        }

        if (q.startAtFrom() != null && q.startAtTo() != null) {
            LocalDateTime from = q.startAtFrom().atStartOfDay();
            LocalDateTime to = q.startAtTo().atTime(23, 59, 59);
            var page = bookingRepository.findByUserIdAndStartDateBetween(q.customerId(), from, to, pageable);
            return page.map(BookingResourceFromEntityAssembler::toResource);
        }

        var page = bookingRepository.findByUserId(q.customerId(), pageable);
        return page.map(BookingResourceFromEntityAssembler::toResource);
    }

    @Override
    public Page<BookingResource> getAllBookings(GetAllBookingsQuery q, Pageable pageable) {
        var all = bookingRepository.findAll(pageable);
        return all.map(BookingResourceFromEntityAssembler::toResource);
    }

    @Override
    public Page<BookingResource> getBookingsByVehicle(GetBookingsByVehicleQuery q, Pageable pageable) {
        var page = bookingRepository.findByUserIdAndVehicleId(q.userId(), q.vehicleId(), pageable);
        return page.map(BookingResourceFromEntityAssembler::toResource);
    }

    @Override
    public Page<BookingResource> getBookingsByStatus(GetBookingsByStatusQuery q, Pageable pageable) {
        var page = bookingRepository.findByUserIdAndStatus(q.userId(), q.status(), pageable);
        return page.map(BookingResourceFromEntityAssembler::toResource);
    }

    @Override
    public Page<BookingResource> getBookingsByUserId(GetBookingsByUserIdQuery q, Pageable pageable) {
        var page = bookingRepository.findByUserId(q.userId(), pageable);
        return page.map(BookingResourceFromEntityAssembler::toResource);
    }

    @Override
    public Page<BookingResource> getPendingBookingsByUser(GetPendingBookingsByUserQuery q, Pageable pageable) {
        int halfSize = Math.max(1, pageable.getPageSize() / 2);
        Pageable halfPageable = PageRequest.of(pageable.getPageNumber(), halfSize, pageable.getSort());

        var pendingPage = bookingRepository.findByUserIdAndStatus(q.userId(), "pending", halfPageable);
        var confirmedPage = bookingRepository.findByUserIdAndStatus(q.userId(), "confirmed", halfPageable);

        var combined = new java.util.ArrayList<>(pendingPage.getContent());
        combined.addAll(confirmedPage.getContent());

        return new PageImpl<>(
            combined.stream()
                .map(BookingResourceFromEntityAssembler::toResource)
                .collect(Collectors.toList()),
            pageable,
            pendingPage.getTotalElements() + confirmedPage.getTotalElements()
        );
    }

    @Override
    public Page<BookingResource> getCompletedBookingsByUser(GetCompletedBookingsByUserQuery q, Pageable pageable) {
        var page = bookingRepository.findByUserIdAndStatus(q.userId(), "completed", pageable);
        return page.map(BookingResourceFromEntityAssembler::toResource);
    }
}
