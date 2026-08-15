package eu.bbmri_eric.negotiator.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.PersistStateChangeListener;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.trigger.Trigger;

@ExtendWith(MockitoExtension.class)
class PersistStateChangeListenerTest {

  @Mock private NegotiationRepository negotiationRepository;
  @Mock private PersonRepository personRepository;
  @Mock private PostRepository postRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private State<String, String> state;
  @Mock private Transition<String, String> transition;
  @Mock private State<String, String> sourceState;
  @Mock private Trigger<String, String> trigger;
  @Mock private StateMachine<String, String> stateMachine;

  private PersistStateChangeListener listener;

  @BeforeEach
  void setUp() {
    listener =
        new PersistStateChangeListener(
            negotiationRepository, personRepository, postRepository, eventPublisher);
  }

  @Test
  void onPersist_negotiationNotFound_publishesEventWithEmptyOrgIds() {
    String negotiationId = "negotiation-unknown";

    when(negotiationRepository.findDetailedById(negotiationId)).thenReturn(Optional.empty());
    when(state.getId()).thenReturn(NegotiationState.IN_PROGRESS.name());
    when(transition.getSource()).thenReturn(sourceState);
    when(sourceState.getId()).thenReturn(NegotiationState.SUBMITTED.name());
    when(transition.getTrigger()).thenReturn(trigger);
    when(trigger.getEvent()).thenReturn(NegotiationEvent.START.name());

    Message<String> message =
        MessageBuilder.withPayload(NegotiationEvent.START.name())
            .setHeader("negotiationId", negotiationId)
            .build();

    listener.onPersist(state, message, transition, stateMachine);

    ArgumentCaptor<NegotiationStateChangeEvent> eventCaptor =
        ArgumentCaptor.forClass(NegotiationStateChangeEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    NegotiationStateChangeEvent published = eventCaptor.getValue();
    assertThat(published.getNegotiationId()).isEqualTo(negotiationId);
    assertThat(published.getInvolvedOrganizationExternalIds()).isEmpty();
  }
}
