package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.Guard;
import eu.bbmri_eric.negotiator.user.PersonService;
import java.util.List;
import org.springframework.stereotype.Component;

/** Guard that allows only representatives of the target resource. */
@Component("isRepresentative")
public class IsRepresentativeGuard implements Guard<ResourceTransitionContext> {

  private final PersonService personService;

  public IsRepresentativeGuard(PersonService personService) {
    this.personService = personService;
  }

  @Override
  public boolean evaluate(ResourceTransitionContext context) {
    if (context.userId() == null) {
      return false;
    }
    return personService.isRepresentativeOfAnyResource(
        context.userId(), List.of(context.resourceId()));
  }
}
