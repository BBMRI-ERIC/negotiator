package eu.bbmri_eric.negotiator.characterization.dump;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.DisablePostsAction;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.EnablePrivatePostsAction;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.EnablePublicPostsAction;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationIsApprovedGuard;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Actions;
import org.springframework.statemachine.guard.Guards;
import reactor.core.publisher.Mono;

/**
 * Tests the reflective unwrap the dump depends on, for all four Guard and Action beans.
 *
 * <p>The one Guard, {@link NegotiationIsApprovedGuard}, sits on no Transition, so the dump never
 * reaches it. Unwrapping it directly shows its absence from the dump is real and not an unwrap
 * failure.
 *
 * <p>The last case checks that an unrecoverable wrapper throws instead of falling back to a
 * placeholder.
 */
class LifecycleGraphDumperUnwrapTest {

  @Test
  @DisplayName("Recovers all three Action bean names from Spring Statemachine's wrapper lambdas")
  void unwrapsEveryActionBean() {
    assertThat(
            LifecycleGraphDumper.unwrapActionBeanName(Actions.from(new EnablePublicPostsAction())))
        .isEqualTo("EnablePublicPostsAction");
    assertThat(
            LifecycleGraphDumper.unwrapActionBeanName(Actions.from(new EnablePrivatePostsAction())))
        .isEqualTo("EnablePrivatePostsAction");
    assertThat(LifecycleGraphDumper.unwrapActionBeanName(Actions.from(new DisablePostsAction())))
        .isEqualTo("DisablePostsAction");
  }

  @Test
  @DisplayName("Recovers the only Guard bean name, which the live graphs never attach")
  void unwrapsTheOnlyGuardBean() {
    assertThat(
            LifecycleGraphDumper.unwrapGuardBeanName(Guards.from(new NegotiationIsApprovedGuard())))
        .isEqualTo("NegotiationIsApprovedGuard");
  }

  @Test
  @DisplayName("Throws rather than degrading when a wrapper captures no Guard or Action bean")
  void throwsWhenTheBeanCannotBeRecovered() {
    Function<StateContext<String, String>, Mono<Boolean>> opaque = context -> Mono.just(true);

    assertThatThrownBy(() -> LifecycleGraphDumper.unwrapGuardBeanName(opaque))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Could not reflectively unwrap the guard bean");
  }
}
