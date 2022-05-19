package eu.bbmri.eric.csit.service.negotiator.configuration;

import eu.bbmri.eric.csit.service.negotiator.model.Person;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditorAwareImpl implements AuditorAware<Person> {

  @Override
  public Optional<Person> getCurrentAuditor() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof NegotiatorJwtAuthenticationToken) {
      return Optional.ofNullable((Person) auth.getPrincipal());
    } else {
      return Optional.empty();
    }
  }
}
