package eu.bbmri.eric.csit.service.negotiator.configuration;

import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetails;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<Person> {

  @Override
  public Optional<Person> getCurrentAuditor() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      return Optional.ofNullable(((NegotiatorUserDetails) auth.getPrincipal()).getPerson());
    } else {
      return Optional.empty();
    }
  }
}
