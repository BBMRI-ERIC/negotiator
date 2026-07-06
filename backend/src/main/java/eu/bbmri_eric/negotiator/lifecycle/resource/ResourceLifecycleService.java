package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceState;
import java.util.Map;
import java.util.Set;

public interface ResourceLifecycleService {

  Set<NegotiationResourceEvent> getPossibleEvents(String negotiationId, String resourceId);

  NegotiationResourceState sendEvent(
      String negotiationId, String resourceId, NegotiationResourceEvent negotiationResourceEvent);

  Map<String, Object> getStateMachineDiagram();
}
