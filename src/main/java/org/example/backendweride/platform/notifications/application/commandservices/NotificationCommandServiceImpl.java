package org.example.backendweride.platform.notifications.application.commandservices;

import org.example.backendweride.platform.notifications.domain.model.aggregates.Notification;
import org.example.backendweride.platform.notifications.domain.model.commands.CreateNotificationCommand;
import org.example.backendweride.platform.notifications.domain.model.commands.MarkNotificationAsReadCommand;
import org.example.backendweride.platform.notifications.domain.model.commands.UpdateNotificationCommand;
import org.example.backendweride.platform.notifications.domain.services.NotificationCommandService;
import org.example.backendweride.platform.notifications.infrastructure.persistence.jpa.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NotificationCommandServiceImpl implements NotificationCommandService {

    private final NotificationRepository notificationRepository;

    public NotificationCommandServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification handle(CreateNotificationCommand command) {
        var notification = new Notification(command);
        notificationRepository.save(notification);
        return notification;
    }

    // Nueva implementación para marcar como leída
    @Override
    public void handle(MarkNotificationAsReadCommand command) {
        notificationRepository.findByPublicId(command.notificationId())
                .filter(notification -> command.userId().equals(notification.getUserId()))
                .map(notification -> {
                    notification.markAsRead();
                    notificationRepository.save(notification);
                    return notification;
                })
                // ponytail: surfaces as 500 until P-10 adds a @RestControllerAdvice — deliberate,
                // not a leak (same path for not-found and not-owned)
                .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + command.notificationId()));
    }

    @Override
    public Optional<Notification> update(String publicId, String userId, UpdateNotificationCommand command) {
        return notificationRepository.findByPublicId(publicId)
                .filter(notification -> userId.equals(notification.getUserId()))
                .map(notification -> {
                    notification.updateFrom(command);
                    return notificationRepository.save(notification);
                });
    }

    @Override
    public boolean delete(String publicId, String userId) {
        var notification = notificationRepository.findByPublicId(publicId)
                .filter(item -> userId.equals(item.getUserId()));
        notification.ifPresent(notificationRepository::delete);
        return notification.isPresent();
    }
}
