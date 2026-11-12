package org.example.backendweride.platform.profile.interfaces;

import org.example.backendweride.platform.iam.application.internal.outboundservices.security.AuthenticatedAccountProvider;
import org.example.backendweride.platform.profile.domain.model.aggregates.Profile;
import org.example.backendweride.platform.profile.domain.model.commands.CreateProfileCommand;
import org.example.backendweride.platform.profile.domain.services.commands.ProfileCommandService;
import org.example.backendweride.platform.profile.domain.services.queries.ProfileQueryService;
import org.example.backendweride.platform.profile.interfaces.resources.CreateProfileCommandResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileCommandService profileCommandService;

    @Mock
    private ProfileQueryService profileQueryService;

    @Mock
    private AuthenticatedAccountProvider authenticatedAccountProvider;

    @InjectMocks
    private ProfileController controller;


    @Test
    void getProfileById_returnsNotFound_whenProfileBelongsToAnotherUser() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);

        Profile foreignProfile = mock(Profile.class);

        when(foreignProfile.getUserId()).thenReturn(999L);
        when(profileQueryService.handle(3L)).thenReturn(Optional.of(foreignProfile));

        // Act
        var response = controller.getProfileById(3L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(profileQueryService).handle(3L);
        verify(authenticatedAccountProvider).getCurrentAccountId();
    }

    @Test
    void createProfile_usesAuthenticatedUserIdNotResourceBody() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);

        var resource = new CreateProfileCommandResource("Ada", "Lovelace", "ada@weride.com");

        ArgumentCaptor<CreateProfileCommand> captor = ArgumentCaptor.forClass(CreateProfileCommand.class);

        Profile createdProfile = mock(Profile.class);

        when(createdProfile.getId()).thenReturn(10L);
        when(createdProfile.getUserId()).thenReturn(42L);
        when(createdProfile.getFirstName()).thenReturn("Ada");
        when(createdProfile.getLastName()).thenReturn("Lovelace");
        when(createdProfile.getEmail()).thenReturn("ada@weride.com");

        when(profileCommandService.handle(any(CreateProfileCommand.class))).thenReturn(Optional.of(createdProfile));

        // Act
        var response = controller.createProfile(resource);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(42L, response.getBody().userId());
        assertEquals("Ada", response.getBody().firstName());
        assertEquals("Lovelace", response.getBody().lastName());
        assertEquals("ada@weride.com", response.getBody().email());

        verify(authenticatedAccountProvider).getCurrentAccountId();
        verify(profileCommandService).handle(captor.capture());

        assertEquals(42L, captor.getValue().userId());
        assertEquals("Ada", captor.getValue().firstName());
        assertEquals("Lovelace", captor.getValue().lastName());
        assertEquals("ada@weride.com", captor.getValue().email());
    }
}
