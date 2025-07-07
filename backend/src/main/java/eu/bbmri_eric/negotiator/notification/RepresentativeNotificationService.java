package eu.bbmri_eric.negotiator.notification;

public interface RepresentativeNotificationService {
  /** Notify Representatives about all Pending Negotiations. */
  void notifyAboutPendingNegotiations();
  void notifyAboutANewNegotiation(String negotiationId);
}
