package eu.bbmri_eric.negotiator.listeners;

import eu.bbmri_eric.negotiator.events.FirstRepresentativeEvent;
import eu.bbmri_eric.negotiator.governance.resource.NonRepresentedResourcesHandler;
import lombok.NonNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/** Listener for FirstRepresentativeEvent. */
@Component
public class FirstRepresentativeListener implements ApplicationListener<FirstRepresentativeEvent> {
  private final NonRepresentedResourcesHandler handler;

  public FirstRepresentativeListener(NonRepresentedResourcesHandler handler) {
    this.handler = handler;
  }

  @Override
  public void onApplicationEvent(@NonNull FirstRepresentativeEvent event) {
    handler.updateResourceInOngoingNegotiations(event.getResourceId(), event.getSourceId());
  }
}
