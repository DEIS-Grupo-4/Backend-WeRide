package org.example.backendweride.platform.notifications.interfaces;

import org.example.backendweride.platform.iam.application.internal.outboundservices.security.AuthenticatedAccountProvider;
import org.example.backendweride.platform.notifications.domain.model.aggregates.Notification;
import org.example.backendweride.platform.notifications.domain.model.commands.MarkNotificationAsReadCommand;
import org.example.backendweride.platform.notifications.domain.model.queries.GetAllNotificationsByUserIdQuery;
import org.example.backendweride.platform.notifications.domain.model.queries.GetNotificationByIdQuery;
import org.example.backendweride.platform.notifications.domain.services.NotificationCommandService;
import org.example.backendweride.platform.notifications.domain.services.NotificationQueryService;
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
class NotificationsControllerTest {

    @Mock
    private NotificationCommandService notificationCommandService;

    @Mock
    private NotificationQueryService notificationQueryService;

    @Mock
    private AuthenticatedAccountProvider authenticatedAccountProvider;

    @InjectMocks
    private NotificationsController controller;

    @Test
    void getAllNotificationsByUserId_usesAuthenticatedUserId_notQueryParam() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(notificationQueryService.handle(any(GetAllNotificationsByUserIdQuery.class))).thenReturn(List.of());

        // Act
        var response = controller.getAllNotificationsByUserId("999");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        ArgumentCaptor<GetAllNotificationsByUserIdQuery> captor =
                ArgumentCaptor.forClass(GetAllNotificationsByUserIdQuery.class);

        verify(notificationQueryService).handle(captor.capture());

        assertEquals("42", captor.getValue().userId());

        verify(authenticatedAccountProvider).getCurrentAccountId();
    }

    @Test
    void getNotificationById_returnsNotFound_whenNotificationBelongsToAnotherUser() {

        // Arrange
        Notification foreignNotification = mock(Notification.class);

        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);
        when(foreignNotification.getUserId()).thenReturn("999");
        when(
                notificationQueryService.handle(
                        any(GetNotificationByIdQuery.class)
                )
        ).thenReturn(Optional.of(foreignNotification));

        // Act
        var response = controller.getNotificationById("notif-001");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ArgumentCaptor<GetNotificationByIdQuery> captor = ArgumentCaptor.forClass(GetNotificationByIdQuery.class);

        verify(notificationQueryService).handle(captor.capture());

        assertEquals("notif-001", captor.getValue().publicId());

        verify(authenticatedAccountProvider).getCurrentAccountId();
    }

    @Test
    void markAsRead_passesAuthenticatedUserId() {

        // Arrange
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);

        ArgumentCaptor<MarkNotificationAsReadCommand> captor =
                ArgumentCaptor.forClass(MarkNotificationAsReadCommand.class);

        // Act
        var response = controller.markAsRead("notif-001");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Notification marked as read", response.getBody());

        verify(notificationCommandService).handle(captor.capture());

        assertEquals("42", captor.getValue().userId());
        assertEquals("notif-001", captor.getValue().notificationId());

        verify(authenticatedAccountProvider).getCurrentAccountId();
    }
}
