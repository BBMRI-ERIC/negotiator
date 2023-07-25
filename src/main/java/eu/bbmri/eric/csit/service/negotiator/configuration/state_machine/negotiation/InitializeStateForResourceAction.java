package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@CommonsLog
public class InitializeStateForResourceAction implements Action<String, String> {

  @Autowired @Lazy NegotiationRepository negotiationRepository;

  @Override
  @Transactional
  public void execute(StateContext<String, String> context) {
    String negotiationId = context.getMessage().getHeaders().get("negotiationId", String.class);
    Negotiation negotiation = negotiationRepository.findDetailedById(negotiationId).orElseThrow();
    negotiation
        .getAllResources()
        .forEach(
            resource ->
                negotiation.setStateForResource(
                    resource.getSourceId(), NegotiationResourceState.SUBMITTED));
    negotiationRepository.save(negotiation);
  }
}
