package eu.bbmri_eric.negotiator.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@CommonsLog
@Tag(name = "Notifications", description = "Retrieve and update user notifications")
@SecurityRequirement(name = "security_auth")
class NotificationController {
  private final UserNotificationService userNotificationService;
  private final NotificationModelAssembler notificationModelAssembler;

  NotificationController(
      UserNotificationService userNotificationService,
      NotificationModelAssembler notificationModelAssembler) {
    this.userNotificationService = userNotificationService;
    this.notificationModelAssembler = notificationModelAssembler;
  }

  @GetMapping(value = "/users/{id}/notifications", produces = MediaTypes.HAL_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Retrieve Notifications for a User")
  PagedModel<EntityModel<NotificationDTO>> getNotificationsByUserId(
      @PathVariable Long id, @Valid @Nullable @ParameterObject NotificationFilters filters) {
    Page<NotificationDTO> notifications;
    try {
      notifications = (Page<NotificationDTO>) userNotificationService.getAllByUserId(id, filters);
    } catch (ClassCastException e) {
      throw new RuntimeException("Server error. Could not retrieve user notifications");
    }
    return notificationModelAssembler.toPagedModel(notifications, filters, id);
  }

  @GetMapping(value = "/notifications/{id}", produces = MediaTypes.HAL_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Retrieve a Notification")
  EntityModel<NotificationDTO> getNotificationsById(@PathVariable Long id) {
    return EntityModel.of(userNotificationService.getById(id));
  }

  @PatchMapping(value = "/users/{id}/notifications", produces = MediaTypes.HAL_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update multiple notifications read status")
  CollectionModel<EntityModel<NotificationDTO>> updateNotifications(
      @PathVariable Long id, @Valid @RequestBody List<NotificationUpdateDTO> updates) {
    List<NotificationDTO> updatedNotifications =
        userNotificationService.updateNotifications(updates);
    return notificationModelAssembler.toCollectionModel(updatedNotifications);
  }
}
