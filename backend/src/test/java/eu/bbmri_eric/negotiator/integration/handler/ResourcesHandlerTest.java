package eu.bbmri_eric.negotiator.integration.handler;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.governance.resource.NonRepresentedResourcesHandler;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
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
    assertEquals("IN_PROGRESS", negotiation.getCurrentState());
    Resource resource = negotiation.getResources().iterator().next();
    negotiation.setStateForResource(resource.getSourceId(), "REPRESENTATIVE_UNREACHABLE");
    handler.updateResourceInOngoingNegotiations(resource.getId(), resource.getSourceId());
    assertEquals(
        "REPRESENTATIVE_CONTACTED", negotiation.getCurrentStateForResource(resource.getSourceId()));
  }

  @Test
  @Transactional
  void updateState_resourceNotUnreachable_noChange() {
    Negotiation negotiation = negotiationRepository.findAll().iterator().next();
    Resource resource = negotiation.getResources().iterator().next();
    negotiation.setStateForResource(resource.getSourceId(), "REPRESENTATIVE_CONTACTED");

    handler.updateResourceInOngoingNegotiations(resource.getId(), resource.getSourceId());

    assertEquals(
        "REPRESENTATIVE_CONTACTED", negotiation.getCurrentStateForResource(resource.getSourceId()));
  }

  @Test
  @Transactional
  void updateState_abandonedNegotiation_noChange() {
    Negotiation negotiation = negotiationRepository.findAll().iterator().next();
    Resource resource = negotiation.getResources().iterator().next();
    negotiation.setStateForResource(resource.getSourceId(), "REPRESENTATIVE_UNREACHABLE");
    negotiation.setCurrentState("ABANDONED");
    assertEquals("ABANDONED", negotiation.getCurrentState());
    handler.updateResourceInOngoingNegotiations(resource.getId(), resource.getSourceId());
    assertEquals(
        "REPRESENTATIVE_UNREACHABLE",
        negotiation.getCurrentStateForResource(resource.getSourceId()));
  }

  @Test
  void addRepresentative_firstRepresentative_eventPublished() throws InterruptedException {
    personService.assignAsRepresentativeForResource(103L, 10L);
    await()
        .atMost(200, MILLISECONDS)
        .untilAsserted(
            () -> {
              assertEquals(1, testEventListener.events.size());
              assertEquals(1, addedRepresentativeTestEventHandler.events.size());
            });
  }

  @Test
  void addRepresentative_emailNotificationEventPublished() throws InterruptedException {
    int before = addedRepresentativeTestEventHandler.events.size();
    personService.assignAsRepresentativeForResource(104L, 10L);
    await()
        .atMost(200, MILLISECONDS)
        .untilAsserted(
            () -> assertEquals(before + 1, addedRepresentativeTestEventHandler.events.size()));
  }
}
