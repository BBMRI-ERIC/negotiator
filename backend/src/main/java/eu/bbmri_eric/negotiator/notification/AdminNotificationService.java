package eu.bbmri_eric.negotiator.notification;

/** Service for processing notifications to admins. */
public interface AdminNotificationService {
  /**
   * Create a notification for each Admin.
   *
   * @param title of the Notification
   * @param message content of the Notification
   */
  void notifyAllAdmins(String title, String message);

  /**
   * Create a notification for each Admin.
   *
   * @param title of the Notification
   * @param message content of the Notification
   * @param negotiationId of the Negotiation associated with the Notification
   */
  void notifyAllAdmins(String title, String message, String negotiationId);
}
