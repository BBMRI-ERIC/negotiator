package eu.bbmri_eric.negotiator.listeners;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.database.repository.NegotiationResourceLifecycleRepository;
import eu.bbmri_eric.negotiator.events.FirstRepresentativeEvent;
import eu.bbmri_eric.negotiator.service.ResourceLifecycleService;
import java.util.List;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class FirstRepresentativeListener implements ApplicationListener<FirstRepresentativeEvent> {
  private final NegotiationResourceLifecycleRepository repository;
  private final ResourceLifecycleService resourceLifecycleService;

  public FirstRepresentativeListener(
      NegotiationResourceLifecycleRepository repository,
      ResourceLifecycleService resourceLifecycleService) {
    this.repository = repository;
    this.resourceLifecycleService = resourceLifecycleService;
  }

  @Override
  public void onApplicationEvent(@NonNull FirstRepresentativeEvent event) {
    List<NegotiationResourceLifecycleRecord> records =
        repository.findAllByResource_IdAndChangedToAAndNegotiation_CurrentState(
            event.getResourceId(),
            NegotiationResourceState.REPRESENTATIVE_UNREACHABLE,
            NegotiationState.IN_PROGRESS);
    for (NegotiationResourceLifecycleRecord record : records) {
      log.info(
          "LIFECYCLE_CHANGE: Resource %s in Negotiation %s was marked as Contacted"
              .formatted(record.getResource().getSourceId(), record.getNegotiation().getId()));
      resourceLifecycleService.sendEvent(
          record.getNegotiation().getId(),
          record.getResource().getSourceId(),
          NegotiationResourceEvent.CONTACT);
    }
  }
}
