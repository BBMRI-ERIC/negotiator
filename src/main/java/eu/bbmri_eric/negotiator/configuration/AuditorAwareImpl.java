package eu.bbmri_eric.negotiator.configuration;

import eu.bbmri_eric.negotiator.user.NegotiatorUserDetails;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditorAwareImpl implements AuditorAware<Person> {

  @Override
  public Optional<Person> getCurrentAuditor() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      try {
        return Optional.ofNullable(((NegotiatorUserDetails) auth.getPrincipal()).getPerson());
      } catch (ClassCastException e) {
        return Optional.empty();
      }

    } else {
      return Optional.empty();
    }
  }
}
