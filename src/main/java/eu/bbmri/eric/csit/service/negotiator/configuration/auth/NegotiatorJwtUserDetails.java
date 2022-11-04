package eu.bbmri.eric.csit.service.negotiator.configuration.auth;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public class NegotiatorJwtUserDetails implements NegotiatorUserDetails {

  private final Person person;

  public NegotiatorJwtUserDetails(
      Person person, Collection<? extends GrantedAuthority> authorities) {
    this.person = person;
  }

  public Object getPrincipal() {
    return getPerson();
  }

  public Person getPerson() {
    return person;
  }
}
