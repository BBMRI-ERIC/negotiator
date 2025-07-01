package eu.bbmri_eric.negotiator.notification;

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
}
