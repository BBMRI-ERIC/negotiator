package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.dto.NotificationDTO;
import java.util.List;

public interface UserNotificationService {

  /**
   * Returns all notifications for a given user.
   *
   * @param userId the id of the user.
   * @return a list of notifications.
   */
  List<NotificationDTO> getNotificationsForUser(Long userId);

  /**
   * Notify all admins of a new negotiation.
   *
   * @param negotiation that was created.
   */
  void notifyAdmins(Negotiation negotiation);
}
