package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.negotiation.NewNegotiationEvent;
import java.util.Map;
import org.flowable.engine.RuntimeService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NegotiationProcessStartListener {

  private final RuntimeService runtimeService;

  public NegotiationProcessStartListener(RuntimeService runtimeService) {
    this.runtimeService = runtimeService;
  }

  @EventListener
  public void onNewNegotiation(NewNegotiationEvent event) {
    runtimeService.startProcessInstanceByKey(
        "negotiation",
        event.getNegotiationId(),
        Map.of(
            "negotiationId", event.getNegotiationId(),
            "initialState", event.getCurrentState().name()));
  }
}
