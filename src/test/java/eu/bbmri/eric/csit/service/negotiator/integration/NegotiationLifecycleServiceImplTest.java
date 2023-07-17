package eu.bbmri.eric.csit.service.negotiator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import eu.bbmri.eric.csit.service.negotiator.integration.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationLifecycleServiceImpl;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationResourceLifecycleServiceImpl;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class NegotiationLifecycleServiceImplTest {

  final NegotiationState INITIAL_STATE = NegotiationState.SUBMITTED;
  final NegotiationState SECOND_STATE = NegotiationState.APPROVED;
  final NegotiationEvent TRANSITION_EVENT = NegotiationEvent.APPROVE;
  final String NEGOTIATION_ID = "negotiationID-1";
  @Autowired NegotiationLifecycleServiceImpl negotiationStateService;
  @Autowired NegotiationResourceLifecycleServiceImpl negotiationResourceLifecycleService;

  @Autowired NegotiationService negotiationService;
  @Autowired JpaStateMachineRepository jpaStateMachineRepository;

  @AfterEach
  void tearDown() {
    negotiationStateService.removeStateMachine(NEGOTIATION_ID);
  }

  @Test
  void getStateForNonExistentNegotiationThrowsIllegalArgException() {
    negotiationStateService.initializeTheStateMachine(NEGOTIATION_ID);
    assertThrows(
        EntityNotFoundException.class, () -> negotiationStateService.getCurrentState("fake"));
  }

  @Test
  public void getStateReturnsInitialValueAfterInitializingStateMachine() {
    negotiationStateService.initializeTheStateMachine(NEGOTIATION_ID);
    assertEquals(INITIAL_STATE, negotiationStateService.getCurrentState(NEGOTIATION_ID));
  }

  @Test
  public void getPossibleEventsForExistingNegotiation() {
    negotiationStateService.initializeTheStateMachine(NEGOTIATION_ID);
    assertEquals(INITIAL_STATE, negotiationStateService.getCurrentState(NEGOTIATION_ID));
    assertEquals(
        Set.of(NegotiationEvent.APPROVE, NegotiationEvent.DECLINE),
        negotiationStateService.getPossibleEvents(NEGOTIATION_ID));
  }

  @Test
  public void getPossibleEventsForNonExistingNegotiation() {
    assertThrows(
        EntityNotFoundException.class, () -> negotiationStateService.getPossibleEvents("fakeId"));
  }

  @Test
  public void sendEventForNonExistentNegotiationThrowException() {
    assertThrows(
        EntityNotFoundException.class,
        () -> negotiationStateService.sendEvent("fakeId", TRANSITION_EVENT));
  }

  @Test
  public void testSendInvalidEventForNegotiation() {
    String negotiationID = "negotiationID-1";
    negotiationStateService.initializeTheStateMachine(negotiationID);
    assertThrows(
        WrongRequestException.class,
        () -> negotiationStateService.sendEvent(negotiationID, NegotiationEvent.ABANDON));
  }

  @Test
  void stateMachineChangeUpdatesNegotiationDTO() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation(Set.of("request-2"));
    NegotiationDTO negotiationDTO = negotiationService.create(negotiationCreateDTO, 101L);
    assertEquals(
        "SUBMITTED", negotiationService.findById(negotiationDTO.getId(), false).getStatus());
    assertEquals(false, negotiationDTO.getPostsEnabled());
    negotiationStateService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(
        true, negotiationService.findById(negotiationDTO.getId(), false).getPostsEnabled());
    assertNotEquals(
        "SUBMITTED", negotiationService.findById(negotiationDTO.getId(), false).getStatus());
  }

  @Test
  void resourceStateMachineChangeUpdatesNegotiationDTO() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation(Set.of("request-2"));
    NegotiationDTO negotiationDTO = negotiationService.create(negotiationCreateDTO, 101L);
    assertEquals(
        NegotiationResourceState.SUBMITTED.name(),
        negotiationService
            .findById(negotiationDTO.getId(), false)
            .getResourceStatus()
            .get("biobank:1:collection:2")
            .textValue());
    negotiationResourceLifecycleService.sendEvent(
        negotiationDTO.getId(), "biobank:1:collection:2", NegotiationResourceEvent.CONTACT);
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED.name(),
        negotiationService
            .findById(negotiationDTO.getId(), false)
            .getResourceStatus()
            .get("biobank:1:collection:2")
            .textValue());
    negotiationResourceLifecycleService.sendEvent(
        negotiationDTO.getId(),
        "biobank:1:collection:2",
        NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY);
    assertEquals(
        NegotiationResourceState.CHECKING_AVAILABILITY.name(),
        negotiationService
            .findById(negotiationDTO.getId(), false)
            .getResourceStatus()
            .get("biobank:1:collection:2")
            .textValue());
  }
}
