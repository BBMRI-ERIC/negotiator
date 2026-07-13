package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.lifecycle.IsAdminGuard;
import eu.bbmri_eric.negotiator.lifecycle.IsCreatorGuard;
import eu.bbmri_eric.negotiator.lifecycle.NegotiatorTransitionContext;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IsCreatorOrAdminGuardTest {

  @Mock IsAdminGuard isAdmin;
  @Mock IsCreatorGuard isCreator;
  private IsCreatorOrAdminGuard guard;

  @BeforeEach
  void setUp() {
    guard = new IsCreatorOrAdminGuard(isAdmin, isCreator);
  }

  @Test
  void evaluate_admin_returnsTrue() {
    NegotiationTransitionContext ctx =
        new NegotiationTransitionContext("neg-1", Set.of("ROLE_ADMIN"), null, 1L);
    when(isAdmin.evaluate(ctx)).thenReturn(true);

    assertThat(guard.evaluate(ctx)).isTrue();
  }

  @Test
  void evaluate_creator_returnsTrue() {
    NegotiationTransitionContext ctx =
        new NegotiationTransitionContext("neg-1", Set.of(), null, 42L);
    when(isAdmin.evaluate(ctx)).thenReturn(false);
    when(isCreator.evaluate(ctx)).thenReturn(true);

    assertThat(guard.evaluate(ctx)).isTrue();
  }

  @Test
  void evaluate_neither_returnsFalse() {
    NegotiationTransitionContext ctx =
        new NegotiationTransitionContext("neg-1", Set.of(), null, 42L);
    when(isAdmin.evaluate(ctx)).thenReturn(false);
    when(isCreator.evaluate(ctx)).thenReturn(false);

    assertThat(guard.evaluate(ctx)).isFalse();
  }
}
