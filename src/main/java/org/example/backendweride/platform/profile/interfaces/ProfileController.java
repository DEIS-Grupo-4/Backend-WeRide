package org.example.backendweride.platform.profile.interfaces;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.backendweride.platform.iam.application.internal.outboundservices.security.AuthenticatedAccountProvider;
import org.example.backendweride.platform.profile.domain.model.commands.UpdateProfileCommand;
import org.example.backendweride.platform.profile.domain.services.commands.ProfileCommandService;
import org.example.backendweride.platform.profile.domain.services.queries.ProfileQueryService;
import org.example.backendweride.platform.profile.interfaces.resources.CreateProfileCommandResource;
import org.example.backendweride.platform.profile.interfaces.resources.ProfileResource;
import org.example.backendweride.platform.profile.interfaces.resources.UpdateProfileResource;
import org.example.backendweride.platform.profile.interfaces.transform.CreateProfileCommandFromResourceAssembler;
import org.example.backendweride.platform.profile.interfaces.transform.ProfileResourceFromEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/profiles", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Profiles", description = "Create and retrieve user profiles")
public class ProfileController {
    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;
    private final AuthenticatedAccountProvider authenticatedAccountProvider;

    public ProfileController(
            ProfileCommandService profileCommandService,
            ProfileQueryService profileQueryService,
            AuthenticatedAccountProvider authenticatedAccountProvider
    ) {
        this.profileCommandService = profileCommandService;
        this.profileQueryService = profileQueryService;
        this.authenticatedAccountProvider = authenticatedAccountProvider;
    }

    /**
     * Creates (or upserts) the profile of the authenticated user.
     */
    @PostMapping
    public ResponseEntity<ProfileResource> createProfile(
            @Valid @RequestBody CreateProfileCommandResource profileResource
    ) {
        var userId = authenticatedAccountProvider.getCurrentAccountId();
        var result = this.profileCommandService.handle(
                CreateProfileCommandFromResourceAssembler.toCommandFromResource(profileResource, userId));

        return result.map(response -> new ResponseEntity<>(
                ProfileResourceFromEntity.tpProfileResourceFromEntity(response), CREATED
        )).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Returns the profile of the authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileResource> getMyProfile() {
        var userId = authenticatedAccountProvider.getCurrentAccountId();
        var result = this.profileQueryService.handleByUserId(userId);
        return result.map(profile -> ResponseEntity.ok(ProfileResourceFromEntity.tpProfileResourceFromEntity(profile)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Updates the profile of the authenticated user.
     */
    @PutMapping("/me")
    public ResponseEntity<ProfileResource> updateMyProfile(@Valid @RequestBody UpdateProfileResource resource) {
        var userId = authenticatedAccountProvider.getCurrentAccountId();
        var command = new UpdateProfileCommand(resource.firstName(), resource.lastName(), resource.email());
        var result = this.profileCommandService.updateProfile(userId, command);
        return result.map(profile -> ResponseEntity.ok(ProfileResourceFromEntity.tpProfileResourceFromEntity(profile)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResource> getProfileById(@PathVariable Long id) {
        var result = this.profileQueryService.handle(id);
        return result.filter(profile -> authenticatedAccountProvider.getCurrentAccountId().equals(profile.getUserId()))
                .map(profile -> ResponseEntity.ok(ProfileResourceFromEntity.tpProfileResourceFromEntity(profile)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

}
