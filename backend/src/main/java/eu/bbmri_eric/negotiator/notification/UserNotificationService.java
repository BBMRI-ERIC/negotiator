package eu.bbmri_eric.negotiator.notification;

import jakarta.annotation.Nonnull;
import java.util.List;

/** Service interface for managing user notifications. */
interface UserNotificationService {

  /**
   * Retrieves all notifications associated with the specified user ID.
   *
   * @param userId the ID of the user whose notifications are to be retrieved
   * @param filters filters for notifications
   * @return a list of {@link NotificationDTO} objects for the given user
   */
  Iterable<NotificationDTO> getAllByUserId(Long userId, NotificationFilters filters);

  /**
   * Retrieves notifications by a specific notification ID.
   *
   * @param id the ID of the notification
   * @return a list containing the matching {@link NotificationDTO}, if found
   */
  NotificationDTO getById(Long id);

  /**
   * Updates the read status of multiple notifications.
   *
   * @param updates list of notification updates containing id and read status
   * @return list of updated NotificationDTO objects
   */
  List<NotificationDTO> updateNotifications(@Nonnull List<NotificationUpdateDTO> updates);
}
