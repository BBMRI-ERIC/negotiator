package eu.bbmri_eric.negotiator.lifecycle.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.user.PersonService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IsRepresentativeGuardTest {

  @Mock PersonService personService;
  private IsRepresentativeGuard guard;

  @BeforeEach
  void setUp() {
    guard = new IsRepresentativeGuard(personService);
  }

  @Test
  void evaluate_representative_returnsTrue() {
    ResourceTransitionContext ctx = ctx(42L);
    when(personService.isRepresentativeOfAnyResource(42L, List.of("biobank:1:collection:1")))
        .thenReturn(true);

    assertThat(guard.evaluate(ctx)).isTrue();
  }

  @Test
  void evaluate_notRepresentative_returnsFalse() {
    ResourceTransitionContext ctx = ctx(42L);
    when(personService.isRepresentativeOfAnyResource(42L, List.of("biobank:1:collection:1")))
        .thenReturn(false);

    assertThat(guard.evaluate(ctx)).isFalse();
  }

  @Test
  void evaluate_nullUserId_returnsFalse() {
    ResourceTransitionContext ctx = ctx(null);

    assertThat(guard.evaluate(ctx)).isFalse();
    verifyNoInteractions(personService);
  }

  private ResourceTransitionContext ctx(Long userId) {
    return new ResourceTransitionContext(
        "neg-1", Set.of(), "biobank:1:collection:1", userId);
  }
}
