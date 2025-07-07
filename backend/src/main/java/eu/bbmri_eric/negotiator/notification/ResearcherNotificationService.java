package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;

public interface ResearcherNotificationService {

  void createConfirmationNotification(String negotiationId);

  void statusChangeNotification(String negotiationId, NegotiationEvent action);

  void statusChangeNotification(String negotiationId, NegotiationEvent action, String post);
}
