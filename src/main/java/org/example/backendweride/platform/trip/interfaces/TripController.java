package org.example.backendweride.platform.trip.interfaces;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.backendweride.platform.iam.application.internal.outboundservices.security.AuthenticatedAccountProvider;
import org.example.backendweride.platform.trip.application.internal.queries.TripQueryServiceImpl;
import org.example.backendweride.platform.trip.domain.services.commands.TripCommandService;
import org.example.backendweride.platform.trip.domain.services.queries.TripQueryService;
import org.example.backendweride.platform.trip.interfaces.resources.CreateTripCommandResource;
import org.example.backendweride.platform.trip.interfaces.resources.TripResource;
import org.example.backendweride.platform.trip.interfaces.resources.UpdateTripResource;
import org.example.backendweride.platform.trip.interfaces.transform.CreateTripCommandFromResourceAssembler;
import org.example.backendweride.platform.trip.interfaces.transform.TripResourceFromEntityAssembler;
import org.example.backendweride.platform.trip.interfaces.transform.UpdateTripCommandFromResourceAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/trips", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Trips", description = "Create, list, and delete trips")
public class TripController {
    private final TripCommandService tripCommandService;
    private final TripQueryService tripQueryService;
    private final AuthenticatedAccountProvider authenticatedAccountProvider;

    public TripController(
            TripCommandService tripCommandService,
            TripQueryService tripQueryService,
            AuthenticatedAccountProvider authenticatedAccountProvider
    ) {
        this.tripCommandService = tripCommandService;
        this.tripQueryService = tripQueryService;
        this.authenticatedAccountProvider = authenticatedAccountProvider;
    }

    @PostMapping
    public ResponseEntity<TripResource> createTrip(@RequestBody CreateTripCommandResource createTripCommandResource) {
        var userId = String.valueOf(authenticatedAccountProvider.getCurrentAccountId());
        var result = this.tripCommandService.handle(
                CreateTripCommandFromResourceAssembler.toCommandFromResource(createTripCommandResource, userId));
        return result.map(response -> new ResponseEntity<>(
                TripResourceFromEntityAssembler.toResource(response), HttpStatus.CREATED
                )).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping
    public ResponseEntity<List<TripResource>> getAllTrips() {
        var result = this.tripQueryService.handle(String.valueOf(authenticatedAccountProvider.getCurrentAccountId()));
        return result.map(response ->
                new ResponseEntity<>(response, HttpStatus.OK
        )).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResource> getTripById(@PathVariable Long id) {
        return tripQueryService.handle(id, String.valueOf(authenticatedAccountProvider.getCurrentAccountId()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TripResource> updateTrip(@PathVariable Long id, @RequestBody UpdateTripResource resource) {
        String userId = String.valueOf(authenticatedAccountProvider.getCurrentAccountId());
        return tripCommandService.handle(id, userId, UpdateTripCommandFromResourceAssembler.toCommand(resource))
                .map(TripResourceFromEntityAssembler::toResource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTripById(@PathVariable Long id) {
        boolean deleted = this.tripCommandService.handle(
                id,
                String.valueOf(authenticatedAccountProvider.getCurrentAccountId())
        );
        return deleted
                ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
