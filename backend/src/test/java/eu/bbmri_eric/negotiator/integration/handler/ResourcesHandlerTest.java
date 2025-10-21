package eu.bbmri_eric.negotiator.integration.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.governance.resource.NonRepresentedResourcesHandler;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.user.PersonService;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest(loadTestData = true)
public class ResourcesHandlerTest {
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired PersonRepository personRepository;
  @Autowired NonRepresentedResourcesHandler handler;
  @Autowired PersonService personService;
  @Autowired TestEventListener testEventListener;
  @Autowired AddedRepresentativeTestEventHandler addedRepresentativeTestEventHandler;
  @Autowired NotificationService notificationService;

  @Test
  @Transactional
  void updateState_1negotiation1Resource_updated() {
    Negotiation negotiation = negotiationRepository.findAll().iterator().next();
    assertEquals("negotiation-1", negotiation.getId());
    assertEquals(NegotiationState.IN_PROGRESS, negotiation.getCurrentState());
    Resource resource = negotiation.getResources().iterator().next();
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);
    handler.updateResourceInOngoingNegotiations(resource.getId(), resource.getSourceId());
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        negotiation.getCurrentStateForResource(resource.getSourceId()));
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
        negotiation.getCurrentStateForResource(resource.getSourceId()));
  }

  @Test
  void addRepresentative_firstRepresentative_eventPublished() throws InterruptedException {
    personService.assignAsRepresentativeForResource(103L, 10L);
    Thread.sleep(100L);
    assertEquals(1, testEventListener.events.size());
    assertEquals(1, addedRepresentativeTestEventHandler.events.size());
  }

  @Test
  void addRepresentative_emailNotificationEventPublished() throws InterruptedException {
    personService.assignAsRepresentativeForResource(104L, 10L);
    Thread.sleep(100L);
    assertEquals(2, addedRepresentativeTestEventHandler.events.size());
  }
}
