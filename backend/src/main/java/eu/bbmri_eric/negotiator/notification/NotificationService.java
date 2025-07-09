package eu.bbmri_eric.negotiator.notification;

import jakarta.annotation.Nonnull;
import java.util.List;

public interface NotificationService {
  List<NotificationDTO> createNotifications(@Nonnull NotificationCreateDTO notificationRequest);

  /**
   * Finds a notification by its unique identifier.
   *
   * @param id the unique identifier of the notification
   * @return the NotificationDTO if found, or null if not found
   */
  NotificationDTO findById(@Nonnull Long id);
}
