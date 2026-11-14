package org.example.backendweride.platform.travelhistory.interfaces;

import org.example.backendweride.platform.iam.application.internal.outboundservices.security.AuthenticatedAccountProvider;
import org.example.backendweride.platform.travelhistory.domain.model.queries.GetAllTravelsHistory;
import org.example.backendweride.platform.travelhistory.domain.model.queries.GetTravelsHistoryById;
import org.example.backendweride.platform.travelhistory.domain.services.commandservices.TravelHistoryCommandService;
import org.example.backendweride.platform.travelhistory.domain.services.queryservices.TravelHistoryQueryService;
import org.example.backendweride.platform.travelhistory.interfaces.resources.TravelHistoryResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelHistoryControllerTest {

    @Mock
    private TravelHistoryCommandService travelHistoryCommandService;

    @Mock
    private TravelHistoryQueryService travelHistoryQueryService;

    @Mock
    private AuthenticatedAccountProvider authenticatedAccountProvider;

    @InjectMocks
    private TravelHistoryController controller;

    private TravelHistoryResource sampleTravelHistory() {
        return new TravelHistoryResource(
                1L,
                1L,
                "San Isidro",
                "Scooter",
                "travel-image.jpg",
                "30 minutos",
                "5 km",
                null
        );
    }

    @Test
    void getAllTravelHistories_returnsResources_whenCurrentUserIsAdmin() {

        // Arrange
        var history = sampleTravelHistory();

        when(authenticatedAccountProvider.isCurrentUserAdmin()).thenReturn(true);
        when(
                travelHistoryQueryService.handle(any(GetAllTravelsHistory.class))
        ).thenReturn(Optional.of(List.of(history)));

        // Act
        var response = controller.getAllTravelHistories();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(history, response.getBody().get(0));

        verify(authenticatedAccountProvider).isCurrentUserAdmin();
        verify(travelHistoryQueryService).handle(any(GetAllTravelsHistory.class));
    }

    @Test
    void getAllTravelHistories_returnsForbidden_whenCurrentUserIsNotAdmin() {

        // Arrange
        when(authenticatedAccountProvider.isCurrentUserAdmin()).thenReturn(false);

        // Act
        var response = controller.getAllTravelHistories();

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        verify(authenticatedAccountProvider).isCurrentUserAdmin();
        verifyNoInteractions(travelHistoryQueryService);
    }

    @Test
    void getTravelHistoryById_returnsResources_whenUserOwnsHistory() {

        // Arrange
        var history = sampleTravelHistory();

        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(1L);
        when(
                travelHistoryQueryService.handle(any(GetTravelsHistoryById.class))
        ).thenReturn(Optional.of(List.of(history)));

        // Act
        var response = controller.getTravelHistoryById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(history, response.getBody().get(0));

        ArgumentCaptor<GetTravelsHistoryById> captor = ArgumentCaptor.forClass(GetTravelsHistoryById.class);

        verify(travelHistoryQueryService).handle(captor.capture());
        assertEquals(1L, captor.getValue().id());
        verify(authenticatedAccountProvider).getCurrentAccountId();
    }
}
