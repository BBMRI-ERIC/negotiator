package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.service.UserNotificationService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/** SSM action for initializing states per Resource. */
@Component
@CommonsLog
public class InitializeStateForResourceAction implements Action<String, String> {

  @Autowired @Lazy NegotiationRepository negotiationRepository;

  @Autowired @Lazy UserNotificationService userNotificationService;

  @Override
  public void execute(StateContext<String, String> context) {
    String negotiationId = context.getMessage().getHeaders().get("negotiationId", String.class);
    Negotiation negotiation = negotiationRepository.findDetailedById(negotiationId).orElseThrow();
    Negotiation finalNegotiation = negotiation;
    negotiation
        .getResources()
        .forEach(
            resource ->
                finalNegotiation.setStateForResource(
                    resource.getSourceId(), NegotiationResourceState.SUBMITTED));
    negotiation = negotiationRepository.save(negotiation);
    userNotificationService.notifyRepresentativesAboutNewNegotiation(negotiation);
  }
}
