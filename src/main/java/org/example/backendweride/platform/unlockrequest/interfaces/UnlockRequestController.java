package org.example.backendweride.platform.unlockrequest.interfaces;

import org.example.backendweride.platform.iam.application.internal.outboundservices.security.AuthenticatedAccountProvider;
import org.example.backendweride.platform.unlockrequest.domain.model.UnlockRequest;
import org.example.backendweride.platform.unlockrequest.infrastructure.UnlockRequestRepository;
import org.example.backendweride.platform.unlockrequest.interfaces.resources.CreateUnlockRequestResource;
import org.example.backendweride.platform.unlockrequest.interfaces.resources.UnlockRequestResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/unlockRequests")
public class UnlockRequestController {
    private final UnlockRequestRepository repository;
    private final AuthenticatedAccountProvider accountProvider;

    /**
     * Creates the unlock request controller with its repository and account provider.
     *
     * @param repository repository for unlock request persistence
     * @param accountProvider provider of the authenticated account
     */
    public UnlockRequestController(UnlockRequestRepository repository, AuthenticatedAccountProvider accountProvider) {
        this.repository = repository;
        this.accountProvider = accountProvider;
    }

    /**
     * Retrieves unlock requests belonging to the authenticated user,
     * optionally filtering them by booking.
     *
     * @return response containing the user's unlock requests
     */
    @GetMapping
    public ResponseEntity<List<UnlockRequestResource>> getAll(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Long bookingId) {
        String currentUser = currentUser();
        var requests = bookingId == null
                ? repository.findAllByUserId(currentUser)
                : repository.findAllByUserIdAndBookingId(currentUser, bookingId);
        return ResponseEntity.ok(requests.stream().map(this::toResource).toList());
    }

    /**
     * Creates a new unlock request for the authenticated user.
     *
     * @param resource information required to create the unlock request
     * @return response containing the created unlock request
     */
    @PostMapping
    public ResponseEntity<UnlockRequestResource> create(@RequestBody CreateUnlockRequestResource resource) {
        var request = new UnlockRequest(
                currentUser(), resource.vehicleId(), resource.bookingId(), resource.requestedAt(),
                resource.scheduledUnlockTime(), resource.actualUnlockTime(), resource.status(), resource.method(),
                resource.location(), resource.unlockCode(), resource.attempts() == null ? 0 : resource.attempts(),
                resource.errorMessage());
        return ResponseEntity.status(201).body(toResource(repository.save(request)));
    }

    /**
     * Retrieves an unlock request by its identifier if it belongs to the current user.
     *
     * @param id unlock request identifier
     * @return response containing the requested unlock request
     */
    @GetMapping("/{id}")
    public ResponseEntity<UnlockRequestResource> getById(@PathVariable String id) {
        return owned(id).map(request -> ResponseEntity.ok(toResource(request)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing unlock request owned by the authenticated user.
     *
     * @param id unlock request identifier
     * @param resource updated unlock request information
     * @return response containing the updated unlock request
     */
    @PatchMapping("/{id}")
    public ResponseEntity<UnlockRequestResource> update(
            @PathVariable String id,
            @RequestBody CreateUnlockRequestResource resource
    ) {
        return owned(id).map(request -> {
            request.update(resource.vehicleId(), resource.bookingId(), resource.scheduledUnlockTime(),
                    resource.actualUnlockTime(), resource.status(), resource.method(), resource.location(),
                    resource.unlockCode(), resource.attempts(), resource.errorMessage());
            return ResponseEntity.ok(toResource(repository.save(request)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes an unlock request belonging to the authenticated user.
     *
     * @param id unlock request identifier
     * @return response indicating the deletion result
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return owned(id).map(request -> {
            repository.delete(request);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private java.util.Optional<UnlockRequest> owned(String publicId) {
        return repository.findByPublicId(publicId).filter(request -> currentUser().equals(request.getUserId()));
    }

    private String currentUser() {
        return String.valueOf(accountProvider.getCurrentAccountId());
    }

    private UnlockRequestResource toResource(UnlockRequest request) {
        return new UnlockRequestResource(request.getPublicId(), request.getUserId(), request.getVehicleId(),
                request.getBookingId(), request.getRequestedAt(), request.getScheduledUnlockTime(),
                request.getActualUnlockTime(), request.getStatus(), request.getMethod(), request.getLocation(),
                request.getUnlockCode(), request.getAttempts(), request.getErrorMessage());
    }
}
