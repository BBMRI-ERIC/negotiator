package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.post.Post;
import java.util.List;

public interface OldNotificationService {

  /**
   * Returns all notifications for a given user.
   *
   * @param userId the id of the user.
   * @return a list of notifications.
   */
  List<NotificationDTO> getNotificationsForUser(Long userId);

  /**
   * Create notifications for all representatives of resources involved in a new negotiation.
   *
   * @param negotiation that was created.
   */
  void notifyRepresentativesAboutNewNegotiation(Negotiation negotiation);

  /**
   * Create notifications for all representatives of resources involved in a new negotiation.
   *
   * @param negotiationId that was created.
   */
  void notifyRepresentativesAboutNewNegotiation(String negotiationId);

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
}
