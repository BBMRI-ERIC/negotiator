package eu.bbmri_eric.negotiator.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.lifecycle.negotiation.NegotiationTransitionContext;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IsCreatorGuardTest {

  @Mock NegotiationRepository negotiationRepository;
  private IsCreatorGuard guard;

  @BeforeEach
  void setUp() {
    guard = new IsCreatorGuard(negotiationRepository);
  }

  @Test
  void evaluate_creator_returnsTrue() {
    NegotiationTransitionContext ctx =
        new NegotiationTransitionContext("neg-1", Set.of(), null, 42L);
    when(negotiationRepository.existsByIdAndCreatedBy_Id("neg-1", 42L)).thenReturn(true);

    assertThat(guard.evaluate(ctx)).isTrue();
  }

  @Test
  void evaluate_nonCreator_returnsFalse() {
    NegotiationTransitionContext ctx =
        new NegotiationTransitionContext("neg-1", Set.of(), null, 42L);
    when(negotiationRepository.existsByIdAndCreatedBy_Id("neg-1", 42L)).thenReturn(false);

    assertThat(guard.evaluate(ctx)).isFalse();
  }

  @Test
  void evaluate_nullUserId_returnsFalse() {
    NegotiationTransitionContext ctx =
        new NegotiationTransitionContext("neg-1", Set.of(), null, null);

    assertThat(guard.evaluate(ctx)).isFalse();
    verifyNoInteractions(negotiationRepository);
  }
}
