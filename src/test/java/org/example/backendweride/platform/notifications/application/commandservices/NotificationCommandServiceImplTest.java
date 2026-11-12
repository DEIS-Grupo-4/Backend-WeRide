package org.example.backendweride.platform.notifications.application.commandservices;

import org.example.backendweride.platform.notifications.domain.model.aggregates.Notification;
import org.example.backendweride.platform.notifications.domain.model.commands.MarkNotificationAsReadCommand;
import org.example.backendweride.platform.notifications.infrastructure.persistence.jpa.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationCommandServiceImpl service;

    @Test
    void markAsRead_throws_whenNotificationBelongsToAnotherUser() {

        // Arrange
        var command = new MarkNotificationAsReadCommand("notif-001", "42");
        Notification foreignNotification = mock(Notification.class);

        when(foreignNotification.getUserId()).thenReturn("999");
        when(notificationRepository.findByPublicId("notif-001")).thenReturn(Optional.of(foreignNotification));

        // Act
        var exception = assertThrows(RuntimeException.class, () -> service.handle(command));

        // Assert
        assertEquals("Notification not found with ID: notif-001", exception.getMessage());

        verify(notificationRepository).findByPublicId("notif-001");
        verify(foreignNotification, never()).markAsRead();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAsRead_marksAsRead_whenOwnedByCurrentUser() {

        // Arrange
        var command = new MarkNotificationAsReadCommand("notif-001", "42");
        Notification ownedNotification = mock(Notification.class);

        when(ownedNotification.getUserId()).thenReturn("42");
        when(notificationRepository.findByPublicId("notif-001")).thenReturn(Optional.of(ownedNotification));

        // Act
        service.handle(command);

        // Assert
        verify(notificationRepository).findByPublicId("notif-001");
        verify(ownedNotification).markAsRead();
        verify(notificationRepository).save(ownedNotification);
    }
}
