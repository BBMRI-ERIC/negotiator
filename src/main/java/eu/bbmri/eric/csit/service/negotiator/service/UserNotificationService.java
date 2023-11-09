package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Notification;
import java.util.List;

public interface UserNotificationService {

  /**
   * Returns all notifications for a given user.
   *
   * @param userId the id of the user.
   * @return a list of notifications.
   */
  List<Notification> getNotificationsForUser(Long userId);
}
