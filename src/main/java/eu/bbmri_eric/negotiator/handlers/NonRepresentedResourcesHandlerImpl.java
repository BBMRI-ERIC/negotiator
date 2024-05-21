package eu.bbmri_eric.negotiator.handlers;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.database.repository.NegotiationResourceLifecycleRepository;
import eu.bbmri_eric.negotiator.service.ResourceLifecycleService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class NonRepresentedResourcesHandlerImpl implements NonRepresentedResourcesHandler {

  private final NegotiationResourceLifecycleRepository repository;
  private final ResourceLifecycleService resourceLifecycleService;

  public NonRepresentedResourcesHandlerImpl(
      NegotiationResourceLifecycleRepository repository,
      ResourceLifecycleService resourceLifecycleService) {
    this.repository = repository;
    this.resourceLifecycleService = resourceLifecycleService;
  }

  @Override
  @Transactional
  public void updateResourceInOngoingNegotiations(Long resourceId, String sourceId) {
    List<NegotiationResourceLifecycleRecord> records = findEligibleNegotiations(resourceId);
    for (NegotiationResourceLifecycleRecord record : records) {
      updateResourceStatus(record);
    }
  }

  private void updateResourceStatus(NegotiationResourceLifecycleRecord record) {
    NegotiationResourceState newStatus =
        resourceLifecycleService.sendEvent(
            record.getNegotiation().getId(),
            record.getResource().getSourceId(),
            NegotiationResourceEvent.CONTACT);
    if (newStatus.equals(NegotiationResourceState.REPRESENTATIVE_CONTACTED)) {
      logSuccess(record);
    } else {
      logError(record);
    }
  }

  private static void logError(NegotiationResourceLifecycleRecord record) {
    log.error(
        "LIFECYCLE_CHANGE: Resource %s in Negotiation %s could not be updated"
            .formatted(record.getResource().getSourceId(), record.getNegotiation().getId()));
  }

  private static void logSuccess(NegotiationResourceLifecycleRecord record) {
    log.info(
        "LIFECYCLE_CHANGE: Representative for Resource %s in Negotiation %s was contacted"
            .formatted(record.getResource().getSourceId(), record.getNegotiation().getId()));
  }

  private List<NegotiationResourceLifecycleRecord> findEligibleNegotiations(Long resourceId) {
    List<NegotiationResourceLifecycleRecord> records =
        repository.findAllByResource_IdAndChangedToAndNegotiation_CurrentState(
            resourceId,
            NegotiationResourceState.REPRESENTATIVE_UNREACHABLE,
            NegotiationState.IN_PROGRESS);
    return records;
  }
}
