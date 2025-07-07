package eu.bbmri_eric.negotiator.notification.admin;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(
    classes = {
      AdminNotificationListener.class,
    })
public class AdminNotificationListenerTest {
  @MockitoBean private AdminNotificationService adminNotificationService;

  @MockitoSpyBean private AdminNotificationListener myListener;

  @Autowired private ApplicationEventPublisher publisher;

  @Test
  void onNew_triggered() {
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, "fakeID", NegotiationState.SUBMITTED, NegotiationEvent.SUBMIT, null);
    publisher.publishEvent(event);
    verify(myListener, timeout(1000)).onSubmittedNegotiation(event);
  }

  @Test
  void onNewNegotiation_Approved_notTriggered() {
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, "fakeID", NegotiationState.APPROVED, NegotiationEvent.APPROVE, null);
    publisher.publishEvent(event);
    verify(myListener, never()).onSubmittedNegotiation(event);
  }
}
