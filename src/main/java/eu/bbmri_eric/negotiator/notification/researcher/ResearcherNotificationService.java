package eu.bbmri_eric.negotiator.notification.researcher;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;

public interface ResearcherNotificationService {
  void createConfirmationNotification(String negotiationId);

  void statusChangeNotification(String negotiationId, NegotiationEvent action);
}
