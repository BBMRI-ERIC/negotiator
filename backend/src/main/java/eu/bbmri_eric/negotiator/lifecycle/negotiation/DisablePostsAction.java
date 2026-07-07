package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("lifecycleDisablePostsAction")
public class DisablePostsAction implements JavaDelegate {

  private final NegotiationService negotiationService;

  public DisablePostsAction(NegotiationService negotiationService) {
    this.negotiationService = negotiationService;
  }

  @Override
  public void execute(DelegateExecution execution) {
    String negotiationId = execution.getVariable("negotiationId", String.class);
    negotiationService.setPublicPostsEnabled(negotiationId, false);
    negotiationService.setPrivatePostsEnabled(negotiationId, false);
  }
}
