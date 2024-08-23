package eu.bbmri_eric.negotiator.integration.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.governance.resource.NonRepresentedResourcesHandler;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.user.PersonService;
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
  @Autowired PersonService personService;
  @Autowired TestEventListener testEventListener;

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

  @Test
  @Transactional
  void updateState_abandonedNegotiation_noChange() {
    Negotiation negotiation = negotiationRepository.findAll().iterator().next();
    Resource resource = negotiation.getResources().iterator().next();
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);
    negotiation.setCurrentState(NegotiationState.ABANDONED);
    assertEquals(NegotiationState.ABANDONED, negotiation.getCurrentState());
    handler.updateResourceInOngoingNegotiations(resource.getId(), resource.getSourceId());
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_UNREACHABLE,
        negotiation.getCurrentStatePerResource().get(resource.getSourceId()));
  }

  @Test
  void addRepresentative_firstRepresentative_eventPublished() throws InterruptedException {
    personService.assignAsRepresentativeForResource(103L, 10L);
    Thread.sleep(100L);
    assertEquals(1, testEventListener.events.size());
  }
}
