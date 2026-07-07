package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import java.util.Set;

/**
 * Flowable-backed implementation of Lifecycle management operations on a Negotiation. Replaces
 * {@code
 * eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationLifecycleService}.
 */
public interface NegotiationLifecycleService {

  Set<NegotiationEvent> getPossibleEvents(String negotiationId) throws EntityNotFoundException;

  NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent)
      throws WrongRequestException, EntityNotFoundException;

  NegotiationState sendEvent(
      String negotiationId, NegotiationEvent negotiationEvent, String message)
      throws WrongRequestException, EntityNotFoundException;
}
