package eu.bbmri_eric.negotiator.lifecycle.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.lifecycle.NegotiatorTransitionContext;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NegotiationInProgressGuardTest {

  @Mock NegotiationRepository negotiationRepository;
  private NegotiationInProgressGuard guard;

  @BeforeEach
  void setUp() {
    guard = new NegotiationInProgressGuard(negotiationRepository);
  }

  @Test
  void evaluate_negotiationInProgress_returnsTrue() {
    NegotiatorTransitionContext ctx = ctx("neg-1");
    when(negotiationRepository.findNegotiationStateById("neg-1"))
        .thenReturn(Optional.of(NegotiationState.IN_PROGRESS));

    assertThat(guard.evaluate(ctx)).isTrue();
  }

  @Test
  void evaluate_negotiationNotInProgress_returnsFalse() {
    NegotiatorTransitionContext ctx = ctx("neg-1");
    when(negotiationRepository.findNegotiationStateById("neg-1"))
        .thenReturn(Optional.of(NegotiationState.SUBMITTED));

    assertThat(guard.evaluate(ctx)).isFalse();
  }

  @Test
  void evaluate_negotiationNotFound_returnsFalse() {
    NegotiatorTransitionContext ctx = ctx("neg-1");
    when(negotiationRepository.findNegotiationStateById("neg-1"))
        .thenReturn(Optional.empty());

    assertThat(guard.evaluate(ctx)).isFalse();
  }

  private NegotiatorTransitionContext ctx(String negotiationId) {
    return new ResourceTransitionContext(negotiationId, Set.of(), "res-1", 1L);
  }
}
