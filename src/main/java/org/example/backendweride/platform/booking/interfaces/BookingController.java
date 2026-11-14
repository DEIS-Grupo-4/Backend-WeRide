package org.example.backendweride.platform.booking.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.example.backendweride.platform.booking.interfaces.resources.CreateBookingResource;
import org.example.backendweride.platform.booking.interfaces.resources.SaveBookingDraftResource;
import org.example.backendweride.platform.booking.interfaces.resources.BookingResource;
import org.example.backendweride.platform.booking.interfaces.resources.UpdateBookingResource;
import org.example.backendweride.platform.booking.interfaces.transform.CreateBookingCommandFromResourceAssembler;
import org.example.backendweride.platform.booking.interfaces.transform.SaveBookingDraftCommandFromResourceAssembler;
import org.example.backendweride.platform.booking.interfaces.transform.UpdateBookingCommandFromResourceAssembler;
import org.example.backendweride.platform.booking.domain.services.BookingCommandService;
import org.example.backendweride.platform.booking.domain.services.BookingQueryService;
import org.example.backendweride.platform.booking.domain.services.BookingDraftService;
import org.example.backendweride.platform.booking.domain.model.commands.SaveBookingDraftCommand;
import org.example.backendweride.platform.booking.domain.model.commands.CreateBookingCommand;
import org.example.backendweride.platform.booking.domain.model.commands.DeleteBookingDraftCommand;
import org.example.backendweride.platform.booking.domain.model.queries.GetBookingByIdQuery;
import org.example.backendweride.platform.booking.domain.model.queries.SearchBookingsQuery;
import org.example.backendweride.platform.booking.domain.model.queries.GetBookingDraftsByCustomerQuery;
import org.example.backendweride.platform.iam.application.internal.outboundservices.security.AuthenticatedAccountProvider;
import org.example.backendweride.platform.booking.domain.model.queries.GetBookingsByUserIdQuery;
import org.example.backendweride.platform.booking.domain.model.queries.GetPendingBookingsByUserQuery;
import org.example.backendweride.platform.booking.domain.model.queries.GetCompletedBookingsByUserQuery;
import org.example.backendweride.platform.booking.interfaces.transform.BookingResourceFromEntityAssembler;

import java.util.Optional;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * BookingController handles HTTP requests related to bookings.
 *
 * @summary This controller provides endpoints for creating bookings, saving drafts,
 *          retrieving bookings by ID, searching bookings, and getting drafts by customer.
 */
@RestController
@RequestMapping(value = "/api/v1/bookings", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Booking", description = "Bookings Manager")
public class BookingController {

    private final BookingCommandService commandService;
    private final BookingQueryService bookingQueryService;
    private final BookingDraftService draftService;
    private final AuthenticatedAccountProvider authenticatedAccountProvider;

    public BookingController(BookingCommandService commandService,
                           BookingQueryService bookingQueryService,
                           BookingDraftService draftService,
                           AuthenticatedAccountProvider authenticatedAccountProvider) {
        this.commandService = commandService;
        this.bookingQueryService = bookingQueryService;
        this.draftService = draftService;
        this.authenticatedAccountProvider = authenticatedAccountProvider;
    }

    /**
     * Save a booking draft.
     * @param resource The booking draft details.
     * @return ResponseEntity containing the saved booking draft or an error status.
     */
    @PostMapping("/draft")
    @Operation(summary = "Save a booking draft", description = "Save a booking draft with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Draft saved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data provided")
    })
    public ResponseEntity<BookingResource> saveDraft(@RequestBody SaveBookingDraftResource resource) {
        Long userId = authenticatedAccountProvider.getCurrentAccountId();
        SaveBookingDraftCommand cmd = SaveBookingDraftCommandFromResourceAssembler.toCommand(resource, userId);
        var result = commandService.saveDraft(cmd);
        if (!result.success()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<BookingResource> resp = bookingQueryService.getBookingById(new GetBookingByIdQuery(result.draftId()));
        return resp.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create a new booking.
     * @param resource The booking details.
     * @return ResponseEntity containing the created booking or an error status.
     */
    @PostMapping
    @Operation(summary = "Create a new booking", description = "Create a new booking with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "404", description = "Booking could not be found")
    })
    public ResponseEntity<BookingResource> createBooking(@RequestBody CreateBookingResource resource) {
        CreateBookingCommand cmd = CreateBookingCommandFromResourceAssembler.toCommand(
                resource, authenticatedAccountProvider.getCurrentAccountId());
        var result = commandService.createBooking(cmd);
        if (!result.success()) return ResponseEntity.badRequest().build();

        Optional<BookingResource> resp =
                bookingQueryService.getBookingById(
                        new GetBookingByIdQuery(result.bookingId())
                );
        return resp.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get a booking by its ID.
     * @param id The unique identifier of the booking.
     * @return ResponseEntity containing the booking or an error status.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID", description = "Retrieve a booking by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResource> getBookingById(@PathVariable("id") Long id) {
        var opt = bookingQueryService.getBookingById(new GetBookingByIdQuery(id));
        return opt.filter(booking -> authenticatedAccountProvider.getCurrentAccountId().equals(booking.userId()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BookingResource> updateBooking(
            @PathVariable Long id,
            @RequestBody UpdateBookingResource resource
    ) {
        var updated = commandService.updateBooking(
                id,
                authenticatedAccountProvider.getCurrentAccountId(),
                UpdateBookingCommandFromResourceAssembler.toCommand(resource));
        return updated
                .map(BookingResourceFromEntityAssembler::toResource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingResource> replaceBooking(
            @PathVariable Long id,
            @RequestBody UpdateBookingResource resource
    ) {
        return updateBooking(id, resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        return commandService.deleteBooking(id, authenticatedAccountProvider.getCurrentAccountId())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * Search bookings belonging to the authenticated user, optionally further filtered
     * by status, vehicle, or a start-date range (checked in that priority order — only
     * one filter applies per request).
     * @param vehicleId Filter by vehicle ID (optional).
     * @param status Filter by booking status (optional).
     * @param startAtFrom Filter bookings starting from this date (optional).
     * @param startAtTo Filter bookings up to this date (optional).
     * @param page Page number for pagination (default is 0).
     * @param size Page size for pagination (default is 20).
     * @return ResponseEntity containing a page of bookings matching the criteria.
     */
    @GetMapping
    @Operation(summary = "Search bookings", description = "Search for bookings based on various criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
    public ResponseEntity<Page<BookingResource>> searchBookings(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String status,
        @RequestParam(required = false) String startAtFrom,
        @RequestParam(required = false) String startAtTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        LocalDate from = null;
        LocalDate to = null;
        try {
            if (startAtFrom != null) from = LocalDate.parse(startAtFrom);
            if (startAtTo != null) to = LocalDate.parse(startAtTo);
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().build();
        }

        SearchBookingsQuery q = new SearchBookingsQuery(
                authenticatedAccountProvider.getCurrentAccountId(),
                vehicleId,
                status,
                from,
                to,
                page,
                size
        );
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        var results = bookingQueryService.searchBookings(q, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Get booking drafts for a specific customer.
     * @param customerId The unique identifier of the customer.
     * @param page Page number for pagination (default is 0).
     * @param size Page size for pagination (default is 20).
     * @return ResponseEntity containing a page of booking drafts for the customer.
     */
    @GetMapping("/drafts")
    @Operation(
            summary = "Get booking drafts by customer",
            description = "Retrieve booking drafts for a specific customer"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking drafts retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid customer ID")
    })
    public ResponseEntity<Page<BookingResource>> getDraftsByCustomer(
        @RequestParam(required = false) Long customerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        GetBookingDraftsByCustomerQuery q = new GetBookingDraftsByCustomerQuery(
                authenticatedAccountProvider.getCurrentAccountId(),
                page,
                size
        );
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        var results = bookingQueryService.getBookingDraftsByCustomer(q, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Get bookings by vehicle ID.
     * @param vehicleId The unique identifier of the vehicle.
     * @param page Page number for pagination (default is 0).
     * @param size Page size for pagination (default is 20).
     * @return ResponseEntity containing a page of bookings for the vehicle.
     */
    @GetMapping("/vehicle/{vehicleId}")
    @Operation(summary = "Get bookings by vehicle", description = "Retrieve bookings for a specific vehicle")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid vehicle ID")
    })
    public ResponseEntity<Page<BookingResource>> getBookingsByVehicle(
        @PathVariable Long vehicleId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var q = new org.example.backendweride.platform.booking.domain.model.queries.GetBookingsByVehicleQuery(
                authenticatedAccountProvider.getCurrentAccountId(), vehicleId, page, size);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        var results = bookingQueryService.getBookingsByVehicle(q, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Get bookings by status.
     * @param status The status of bookings to retrieve (draft, confirmed, cancelled).
     * @param page Page number for pagination (default is 0).
     * @param size Page size for pagination (default is 20).
     * @return ResponseEntity containing a page of bookings with the specified status.
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get bookings by status", description = "Retrieve bookings with a specific status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status")
    })
    public ResponseEntity<Page<BookingResource>> getBookingsByStatus(
        @PathVariable String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var q = new org.example.backendweride.platform.booking.domain.model.queries.GetBookingsByStatusQuery(
                authenticatedAccountProvider.getCurrentAccountId(), status, page, size);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        var results = bookingQueryService.getBookingsByStatus(q, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Delete a booking draft.
     * @param draftId The unique identifier of the draft to delete.
     * @return ResponseEntity with the result of the deletion.
     */
    @DeleteMapping("/draft/{draftId}")
    @Operation(summary = "Delete a booking draft", description = "Delete a booking draft by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Draft deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Draft not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<String> deleteDraft(@PathVariable Long draftId) {
        DeleteBookingDraftCommand cmd = new DeleteBookingDraftCommand(
                draftId,
                authenticatedAccountProvider.getCurrentAccountId()
        );
        var result = draftService.deleteDraft(cmd);

        if (!result.success()) {
            return ResponseEntity.badRequest().body(result.message());
        }

        return ResponseEntity.ok(result.message());
    }

    /**
     * Get all bookings for a specific user.
     * @param userId The unique identifier of the user.
     * @param page Page number for pagination (default is 0).
     * @param size Page size for pagination (default is 20).
     * @return ResponseEntity containing a page of bookings for the user.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get bookings by user ID", description = "Retrieve all bookings for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID")
    })
    public ResponseEntity<Page<BookingResource>> getBookingsByUserId(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var q = new GetBookingsByUserIdQuery(
                authenticatedAccountProvider.getCurrentAccountId()
        );
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        var results = bookingQueryService.getBookingsByUserId(q, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Get pending bookings for a specific user.
     * @param userId The unique identifier of the user.
     * @param page Page number for pagination (default is 0).
     * @param size Page size for pagination (default is 20).
     * @return ResponseEntity containing a page of pending bookings for the user.
     */
    @GetMapping("/user/{userId}/pending")
    @Operation(summary = "Get pending bookings by user", description = "Retrieve pending bookings for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending bookings retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID")
    })
    public ResponseEntity<Page<BookingResource>> getPendingBookingsByUser(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var q = new GetPendingBookingsByUserQuery(
                authenticatedAccountProvider.getCurrentAccountId()
        );
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        var results = bookingQueryService.getPendingBookingsByUser(q, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Get completed bookings for a specific user.
     * @param userId The unique identifier of the user.
     * @param page Page number for pagination (default is 0).
     * @param size Page size for pagination (default is 20).
     * @return ResponseEntity containing a page of completed bookings for the user.
     */
    @GetMapping("/user/{userId}/completed")
    @Operation(summary = "Get completed bookings by user",
            description = "Retrieve completed bookings for a specific user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Completed bookings retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID")
    })
    public ResponseEntity<Page<BookingResource>> getCompletedBookingsByUser(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var q = new GetCompletedBookingsByUserQuery(
                authenticatedAccountProvider.getCurrentAccountId()
        );
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        var results = bookingQueryService.getCompletedBookingsByUser(q, pageable);
        return ResponseEntity.ok(results);
    }

}
