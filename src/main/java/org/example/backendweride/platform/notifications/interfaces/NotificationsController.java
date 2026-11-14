package org.example.backendweride.platform.notifications.interfaces;

import org.example.backendweride.platform.iam.application.internal.outboundservices.security.AuthenticatedAccountProvider;
import org.example.backendweride.platform.notifications.domain.model.queries.GetAllNotificationsByUserIdQuery;
import org.example.backendweride.platform.notifications.domain.model.queries.GetNotificationByIdQuery;
import org.example.backendweride.platform.notifications.domain.services.NotificationCommandService;
import org.example.backendweride.platform.notifications.domain.services.NotificationQueryService;
import org.example.backendweride.platform.notifications.interfaces.resources.CreateNotificationResource;
import org.example.backendweride.platform.notifications.interfaces.resources.NotificationResource;
import org.example.backendweride.platform.notifications.interfaces.resources.UpdateNotificationResource;
import org.example.backendweride.platform.notifications.interfaces.transform.CreateNotificationCommandFromResourceAssembler;
import org.example.backendweride.platform.notifications.interfaces.transform.NotificationResourceFromEntityAssembler;
import org.example.backendweride.platform.notifications.interfaces.transform.UpdateNotificationCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.backendweride.platform.notifications.domain.model.commands.MarkNotificationAsReadCommand;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/notifications", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Notification Management Endpoints")
public class NotificationsController {

    private final NotificationCommandService notificationCommandService;
    private final NotificationQueryService notificationQueryService;
    private final AuthenticatedAccountProvider authenticatedAccountProvider;

    /**
     * Creates the notifications controller with its application services.
     *
     * @param notificationCommandService service for notification commands
     * @param notificationQueryService service for notification queries
     * @param authenticatedAccountProvider provider of the authenticated account
     */
    public NotificationsController(
            NotificationCommandService notificationCommandService,
            NotificationQueryService notificationQueryService,
            AuthenticatedAccountProvider authenticatedAccountProvider
    ) {
        this.notificationCommandService = notificationCommandService;
        this.notificationQueryService = notificationQueryService;
        this.authenticatedAccountProvider = authenticatedAccountProvider;
    }

    /**
     * Creates a notification for the authenticated user.
     *
     * @param resource information required to create the notification
     * @return response containing the created notification
     */
    @PostMapping
    public ResponseEntity<NotificationResource> createNotification(@RequestBody CreateNotificationResource resource) {
        var command = CreateNotificationCommandFromResourceAssembler.toCommandFromResource(
                resource, String.valueOf(authenticatedAccountProvider.getCurrentAccountId()));

        var notification = notificationCommandService.handle(command);
        var resourceResult = NotificationResourceFromEntityAssembler.toResourceFromEntity(notification);

        return ResponseEntity.status(HttpStatus.CREATED).body(resourceResult);
    }

    /**
     * Obtener notificaciones por Usuario.
     * Uso: GET /api/v1/notifications?userId=1
     */
    @GetMapping
    public ResponseEntity<List<NotificationResource>> getAllNotificationsByUserId(@RequestParam String userId) {
        var query = new GetAllNotificationsByUserIdQuery(
                String.valueOf(authenticatedAccountProvider.getCurrentAccountId())
        );
        var notifications = notificationQueryService.handle(query);

        var resources = notifications.stream()
                .map(NotificationResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    /**
     * Obtener notificación por ID público.
     * Uso: GET /api/v1/notifications/notif-001
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResource> getNotificationById(@PathVariable String notificationId) {
        var query = new GetNotificationByIdQuery(notificationId);
        var notification = notificationQueryService.handle(query);

        return notification
                .filter(entity ->
                        String.valueOf(authenticatedAccountProvider.getCurrentAccountId())
                                .equals(entity.getUserId())
                )
                .map(entity -> ResponseEntity.ok(NotificationResourceFromEntityAssembler.toResourceFromEntity(entity)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Marks a notification as read.
     *
     * @param notificationId identifier of the notification to update
     * @return response indicating the read status update
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<String> markAsRead(@PathVariable String notificationId) {
        var command = new MarkNotificationAsReadCommand(
                notificationId,
                String.valueOf(authenticatedAccountProvider.getCurrentAccountId())
        );
        notificationCommandService.handle(command);
        return ResponseEntity.ok("Notification marked as read");
    }

    /**
     * Updates an existing notification belonging to the authenticated user.
     *
     * @return response containing the updated notification
     */
    @PatchMapping("/{notificationId}")
    public ResponseEntity<NotificationResource> updateNotification(
            @PathVariable String notificationId,
            @RequestBody UpdateNotificationResource resource) {
        return notificationCommandService.update(
                        notificationId,
                        String.valueOf(authenticatedAccountProvider.getCurrentAccountId()),
                        UpdateNotificationCommandFromResourceAssembler.toCommand(resource))
                .map(NotificationResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes a notification belonging to the authenticated user.
     *
     * @param notificationId identifier of the notification to delete
     * @return response indicating the deletion result
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String notificationId) {
        return notificationCommandService.delete(
                        notificationId,
                        String.valueOf(authenticatedAccountProvider.getCurrentAccountId()))
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
