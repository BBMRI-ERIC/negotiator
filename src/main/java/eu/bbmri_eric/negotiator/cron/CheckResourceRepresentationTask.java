package eu.bbmri_eric.negotiator.cron;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.NegotiationResourceLifecycleRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.events.FirstRepresentativeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CheckResourceRepresentationTask {
  ResourceRepository resourceRepository;
  NegotiationResourceLifecycleRepository negotiationResourceLifecycleRepository;
  ApplicationEventPublisher applicationEventPublisher;

  @Scheduled(cron = "@weekly")
  void checkAllResourcesForNonReachableState() {
    for (Resource resource : resourceRepository.findAll()) {
      boolean exists =
          negotiationResourceLifecycleRepository
              .existsByResource_IdAndChangedToAndNegotiation_CurrentState(
                  resource.getId(),
                  NegotiationResourceState.REPRESENTATIVE_UNREACHABLE,
                  NegotiationState.IN_PROGRESS);
      if (exists) {
        applicationEventPublisher.publishEvent(
            new FirstRepresentativeEvent(this, resource.getId(), resource.getSourceId()));
      }
    }
  }
}
