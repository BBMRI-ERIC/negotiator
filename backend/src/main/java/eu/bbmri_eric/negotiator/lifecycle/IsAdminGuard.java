package eu.bbmri_eric.negotiator.lifecycle;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.Guard;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Guard that allows only callers with the {@code ROLE_ADMIN} authority. */
@Component("isAdmin")
public class IsAdminGuard implements Guard<NegotiatorTransitionContext> {

  @Override
  public boolean evaluate(NegotiatorTransitionContext context) {
    return context != null
        && Objects.nonNull(context.roles())
        && context.roles().contains("ROLE_ADMIN");
  }
}
