package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationLifecycleService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NegotiationLifecycleServiceTest {

  private static NegotiationLifecycleService negotiationLifecycleService;

  final NegotiationState INITIAL_STATE = NegotiationState.SUBMITTED;
  final NegotiationState SECOND_STATE = NegotiationState.APPROVED;

  final NegotiationEvent TRANSITION_EVENT = NegotiationEvent.APPROVE;

  @BeforeEach
  void beforeEach() {
    negotiationLifecycleService =
        new NegotiationLifecycleService() {

          final Map<String, NegotiationState> negotiations = new HashMap<>();

          final Map<String, NegotiationState> resourceNegotiations = new HashMap<>();

          @Override
          public void initializeTheStateMachine(String negotiationId) {
            negotiations.put(negotiationId, INITIAL_STATE);
          }

          @Override
          public NegotiationState getCurrentState(String negotiationId) {
            if (negotiations.get(negotiationId) != null) {
              return negotiations.get(negotiationId);
            } else {
              throw new IllegalArgumentException("Negotiation does not exist");
            }
          }

          @Override
          public Set<NegotiationEvent> getPossibleEvents(String negotiationId) {
            if (negotiations.get(negotiationId) != null) {
              return Set.of(TRANSITION_EVENT);
            } else {
              throw new IllegalArgumentException("Negotiation does not exist");
            }
          }

          @Override
          public NegotiationState sendEvent(
              String negotiationId, NegotiationEvent negotiationEvent) {
            if (negotiations.get(negotiationId) != null && negotiationEvent == TRANSITION_EVENT) {
              negotiations.put(negotiationId, SECOND_STATE);
              return negotiations.get(negotiationId);
            } else if (negotiationEvent != TRANSITION_EVENT) {
              throw new UnsupportedOperationException("Unsupported event");
            } else {
              throw new IllegalArgumentException("Negotiation does not exist");
            }
          }
        };
  }

  @Test
  void getStateForNonExistentNegotiationThrowsIllegalArgException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> negotiationLifecycleService.getCurrentState("fakeId"));
  }

  @Test
  public void getStateReturnsInitialValueAfterInitializingStateMachine() {
    negotiationLifecycleService.initializeTheStateMachine("negotiationID-1");
    assertEquals(INITIAL_STATE, negotiationLifecycleService.getCurrentState("negotiationID-1"));
  }

  @Test
  public void getPossibleEventsForExistingNegotiation() {
    negotiationLifecycleService.initializeTheStateMachine("negotiationID-1");
    assertEquals(
        Set.of(NegotiationEvent.APPROVE),
        negotiationLifecycleService.getPossibleEvents("negotiationID-1"));
  }

  @Test
  public void getPossibleEventsForNonExistingNegotiation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> negotiationLifecycleService.getPossibleEvents("fakeId"));
  }

  @Test
  public void sendValidEventReturnsNewStateAndIsEqualToTheCurrentState() {
    String negotiationID = "negotiationID-1";
    negotiationLifecycleService.initializeTheStateMachine(negotiationID);
    assertEquals(
        SECOND_STATE, negotiationLifecycleService.sendEvent(negotiationID, TRANSITION_EVENT));
    assertEquals(SECOND_STATE, negotiationLifecycleService.getCurrentState(negotiationID));
  }

  @Test
  public void sendEventForNonExistentNegotiationThrowException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> negotiationLifecycleService.sendEvent("fakeId", TRANSITION_EVENT));
  }

  @Test
  public void testSendInvalidEventForNegotiation() {
    String negotiationID = "negotiationID-1";
    negotiationLifecycleService.initializeTheStateMachine(negotiationID);
    assertThrows(
        UnsupportedOperationException.class,
        () -> negotiationLifecycleService.sendEvent(negotiationID, NegotiationEvent.ABANDON));
  }
}
