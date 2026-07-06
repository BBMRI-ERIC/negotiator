package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import java.util.Set;

public interface NegotiationLifecycleService {

  Set<NegotiationEvent> getPossibleEvents(String negotiationId);

  NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent);

  NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent, String message);
}
