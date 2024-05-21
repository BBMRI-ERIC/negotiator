package eu.bbmri_eric.negotiator.integration.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.handlers.NonRepresentedResourcesHandler;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ResourcesHandlerTest {
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired NonRepresentedResourcesHandler handler;

  @Test
  @Transactional
  void updateState_1negotiation1Resource_updated() {
    Negotiation negotiation = negotiationRepository.findAll().iterator().next();
    assertEquals(NegotiationState.IN_PROGRESS, negotiation.getCurrentState());
    Resource resource = negotiation.getResources().iterator().next();
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);
    handler.updateResourceInOngoingNegotiations(resource.getId(), resource.getSourceId());
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        negotiation.getCurrentStatePerResource().get(resource.getSourceId()));
  }
}
