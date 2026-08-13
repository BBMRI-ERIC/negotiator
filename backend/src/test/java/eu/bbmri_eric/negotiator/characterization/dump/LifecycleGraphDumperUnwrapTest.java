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
 * Proves the reflective unwrap on which the whole dump's faithfulness rests.
 *
 * <p>There are exactly four Guard and Action beans in the system, and all four are covered here.
 * The single Guard, {@link NegotiationIsApprovedGuard}, is covered here rather than through the
 * dump because walking the live beans shows it attached to no Transition at all - see the findings
 * on issue 01. Testing it directly is what makes that a finding rather than an unwrap failure that
 * happened to look like one.
 *
 * <p>The last case pins the refusal contract: an unrecoverable wrapper throws rather than degrading
 * to a placeholder, so a partially faithful dump can never be written.
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
