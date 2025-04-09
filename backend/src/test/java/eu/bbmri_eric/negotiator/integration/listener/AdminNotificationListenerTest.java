package eu.bbmri_eric.negotiator.integration.listener;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.notification.admin.AdminNotificationListener;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
public class AdminNotificationListenerTest {

  @Autowired ApplicationEventPublisher eventPublisher;

  @MockitoSpyBean private AdminNotificationListener listener;

  @BeforeEach
  void setUp() {
    reset(listener);
  }

  @Test
  void testListenerMethodIsCalled() {
    eventPublisher.publishEvent(
        new NegotiationStateChangeEvent(
            this, "negotiationId", NegotiationState.SUBMITTED, NegotiationEvent.SUBMIT, null));

    await()
        .atMost(1, SECONDS)
        .until(
            () -> {
              try {
                verify(listener, times(1)).handleSubmittedNegotiation(any());
                return true;
              } catch (AssertionError e) {
                return false;
              }
            });
  }
}
