package org.example.backendweride.platform.location.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.backendweride.platform.location.domain.services.commandservices.LocationCommandService;
import org.example.backendweride.platform.location.domain.services.queryservices.LocationQueryService;
import org.example.backendweride.platform.location.interfaces.resources.CreateLocationResource;
import org.example.backendweride.platform.location.interfaces.resources.LocationResource;
import org.example.backendweride.platform.location.interfaces.transform.CreateLocationCommandFromResourceAssembler;
import org.example.backendweride.platform.location.interfaces.transform.LocationResourceFromEntityAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/location", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Locations", description = "Manage garage/pickup locations")
public class LocationController {
    private final LocationCommandService locationCommandService;
    private final LocationQueryService locationQueryService;

    public LocationController(
            LocationCommandService locationCommandService,
            LocationQueryService locationQueryService
    ) {
        this.locationQueryService = locationQueryService;
        this.locationCommandService = locationCommandService;
    }
    @PostMapping
    @Operation(
            summary = "Crear location",
            description = "Crea una nueva ubicación. Devuelve 201 con Location header y sin cuerpo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created - Location header con URI del recurso"),
            @ApiResponse(responseCode = "400", description = "Bad Request - payload inválido"),
            @ApiResponse(responseCode = "404", description = "Not Found - no se pudo crear")
    })
    public ResponseEntity<LocationResource> createLocation(@RequestBody CreateLocationResource locationResource) {
        var result = this.locationCommandService.handle(
                CreateLocationCommandFromResourceAssembler.toCommandFromResource(locationResource)
        );
        return result.map(LocationResourceFromEntityAssembler::toResourceFromEntity)
                .map(resource -> ResponseEntity.status(HttpStatus.CREATED).body(resource))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(
            summary = "Obtener todas las locations",
            description = "Devuelve 200 con la lista o 204 No Content si no hay resultados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK - lista de locations"),
            @ApiResponse(responseCode = "204", description = "No Content - no hay locations"),
            @ApiResponse(responseCode = "404", description = "Not Found - error de consulta")
    })
    public ResponseEntity<List<LocationResource>> getAllLocation() {
        var result = this.locationQueryService.handle();
        return result.map(response ->
                new ResponseEntity<>(response, HttpStatus.OK
                )).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());

    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResource> getLocation(@PathVariable Long id) {
        return locationQueryService.handle(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationResource> updateLocation(
            @PathVariable Long id,
            @RequestBody CreateLocationResource resource
    ) {
        return locationCommandService.handle(
                id,
                        CreateLocationCommandFromResourceAssembler.toCommandFromResource(resource)
                )
                .map(LocationResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LocationResource> patchLocation(
            @PathVariable Long id,
            @RequestBody CreateLocationResource resource
    ) {
        return updateLocation(id, resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        return locationCommandService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
