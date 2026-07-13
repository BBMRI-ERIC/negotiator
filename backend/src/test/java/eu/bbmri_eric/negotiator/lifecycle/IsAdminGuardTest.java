package eu.bbmri_eric.negotiator.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.negotiator.lifecycle.negotiation.NegotiationTransitionContext;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IsAdminGuardTest {

  private final IsAdminGuard guard = new IsAdminGuard();

  @Test
  void evaluate_adminRole_returnsTrue() {
    NegotiationTransitionContext ctx =
        new NegotiationTransitionContext("neg-1", Set.of("ROLE_ADMIN"), null, 1L);

    assertThat(guard.evaluate(ctx)).isTrue();
  }

  @Test
  void evaluate_noAdminRole_returnsFalse() {
    NegotiationTransitionContext ctx =
        new NegotiationTransitionContext("neg-1", Set.of("ROLE_RESEARCHER"), null, 1L);

    assertThat(guard.evaluate(ctx)).isFalse();
  }

  @Test
  void evaluate_emptyRoles_returnsFalse() {
    NegotiationTransitionContext ctx =
        new NegotiationTransitionContext("neg-1", Set.of(), null, 1L);

    assertThat(guard.evaluate(ctx)).isFalse();
  }

  @Test
  void evaluate_nullRoles_returnsFalse() {
    NegotiationTransitionContext ctx =
        new NegotiationTransitionContext("neg-1", null, null, 1L);

    assertThat(guard.evaluate(ctx)).isFalse();
  }
}
