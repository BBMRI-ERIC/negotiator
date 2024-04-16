package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Post;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.dto.NotificationDTO;
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

  /**
   * Create notifications for all representatives of resources involved in a new negotiation.
   *
   * @param negotiation that was created.
   */
  void notifyRepresentativesAboutNewNegotiation(Negotiation negotiation);

  /**
   * Create a notification of a resource status change for the author of the request.
   *
   * @param negotiation that was updated.
   */
  void notifyRequesterAboutStatusChange(Negotiation negotiation, Resource resource);

  /**
   * Create notifications for all relevant Users about a new Post.
   *
   * @param post that was created.
   */
  void notifyUsersAboutNewPost(Post post);

  /** Send out emails for all pending notifications. */
  void sendEmailsForNewNotifications();

  /** Send out reminder emails for pending and stale negotiations. */
  void createRemindersOldNegotiations();
}
