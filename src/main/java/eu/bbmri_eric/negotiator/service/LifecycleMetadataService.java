package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.NegotiationStateMetadata;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationEventMetadataDto;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationStateMetadataDto;
import java.util.Set;

public interface LifecycleMetadataService {
  /**
   * Retrieve {@link NegotiationState} metadata.
   *
   * @param stateId id of the state.
   * @return {@link NegotiationStateMetadataDto}
   */
  NegotiationStateMetadataDto findStateById(Long stateId);

  /**
   * Retrieve metadata for all {@link NegotiationState}.
   *
   * @return a set of{@link NegotiationStateMetadata}
   */
  Set<NegotiationStateMetadataDto> findAllStates();

  /**
   * Retrieve metadata for events.
   *
   * @param eventId id of the event
   * @return meta
   */
  NegotiationEventMetadataDto findEventById(Long eventId);

  /**
   * Retrieve metadata for all Events.
   *
   * @return a set of Events
   */
  Set<NegotiationEventMetadataDto> findAllEvents();
}
