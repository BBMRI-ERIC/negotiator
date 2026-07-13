package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.lifecycle.IsAdminGuard;
import eu.bbmri_eric.negotiator.lifecycle.IsCreatorGuard;
import eu.bbmri_eric.negotiator.lifecycle.NegotiatorTransitionContext;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.Guard;
import org.springframework.stereotype.Component;

/** Machine guard that allows the negotiation creator or an admin. */
@Component("isCreatorOrAdmin")
public class IsCreatorOrAdminGuard implements Guard<NegotiatorTransitionContext> {

  private final IsAdminGuard isAdmin;
  private final IsCreatorGuard isCreator;

  public IsCreatorOrAdminGuard(IsAdminGuard isAdmin, IsCreatorGuard isCreator) {
    this.isAdmin = isAdmin;
    this.isCreator = isCreator;
  }

  @Override
  public boolean evaluate(NegotiatorTransitionContext context) {
    return isAdmin.evaluate(context) || isCreator.evaluate(context);
  }
}
