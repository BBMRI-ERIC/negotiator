package eu.bbmri_eric.negotiator.handlers;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.service.ResourceLifecycleService;
import jakarta.transaction.Transactional;
import java.util.Objects;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class NonRepresentedResourcesHandlerImpl implements NonRepresentedResourcesHandler {

  private final NegotiationRepository negotiationRepository;
  private final ResourceLifecycleService resourceLifecycleService;

  public NonRepresentedResourcesHandlerImpl(
      NegotiationRepository negotiationRepository,
      ResourceLifecycleService resourceLifecycleService) {
    this.negotiationRepository = negotiationRepository;
    this.resourceLifecycleService = resourceLifecycleService;
  }

  @Override
  @Transactional
  public void updateResourceInOngoingNegotiations(Long resourceId, String sourceId) {
    for (Negotiation negotiation :
        negotiationRepository.findAllByCurrentState(NegotiationState.IN_PROGRESS)) {
      if (Objects.equals(
          negotiation.getCurrentStatePerResource().get(sourceId),
          NegotiationResourceState.REPRESENTATIVE_UNREACHABLE)) {
        updateResourceStatus(negotiation.getId(), sourceId);
      }
    }
  }

  private void updateResourceStatus(String negotiationId, String sourceId) {
    NegotiationResourceState newStatus =
        resourceLifecycleService.sendEvent(
            negotiationId, sourceId, NegotiationResourceEvent.CONTACT);
    if (newStatus.equals(NegotiationResourceState.REPRESENTATIVE_CONTACTED)) {
      logSuccess(negotiationId, sourceId);
    } else {
      logError(negotiationId, sourceId);
    }
  }

  private static void logError(String negotiationId, String sourceId) {
    log.error(
        "LIFECYCLE_CHANGE: Resource %s in Negotiation %s could not be updated"
            .formatted(sourceId, negotiationId));
  }

  private static void logSuccess(String negotiationId, String sourceId) {
    log.info(
        "LIFECYCLE_CHANGE: Representative for Resource %s in Negotiation %s was contacted"
            .formatted(sourceId, negotiationId));
  }
}
