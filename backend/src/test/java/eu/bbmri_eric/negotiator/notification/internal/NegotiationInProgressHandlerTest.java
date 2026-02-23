package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NegotiationInProgressHandlerTest {

  @Mock private ResourceNotificationService resourceNotificationService;

  private NegotiationInProgressHandler handler;

  @BeforeEach
  void setUp() {
    handler = new NegotiationInProgressHandler(resourceNotificationService);
  }

  @Test
  void getSupportedEventType_ReturnsCorrectEventType() {
    Class<NegotiationStateChangeEvent> eventType = handler.getSupportedEventType();
    assertEquals(NegotiationStateChangeEvent.class, eventType);
  }

  @Test
  void notify_WhenNegotiationMovesToInProgress_CallsResourceNotificationService() {
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            negotiationId,
            NegotiationState.SUBMITTED,
            NegotiationState.IN_PROGRESS,
            NegotiationEvent.APPROVE,
            "Test post");

    handler.notify(event);

    verify(resourceNotificationService).notifyResourceRepresentatives(negotiationId);
  }

  @Test
  void notify_WhenNegotiationMovesToOtherState_DoesNotCallService() {
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            negotiationId,
            NegotiationState.DRAFT,
            NegotiationState.SUBMITTED,
            NegotiationEvent.SUBMIT,
            "Test post");

    handler.notify(event);

    verify(resourceNotificationService, never()).notifyResourceRepresentatives(anyString());
  }

  @Test
  void notify_WithDifferentNegotiationId_CallsServiceWithCorrectId() {
    String negotiationId = "NEG-456";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            negotiationId,
            NegotiationState.SUBMITTED,
            NegotiationState.IN_PROGRESS,
            NegotiationEvent.APPROVE,
            "Test post");

    handler.notify(event);

    verify(resourceNotificationService).notifyResourceRepresentatives(negotiationId);
  }
}
