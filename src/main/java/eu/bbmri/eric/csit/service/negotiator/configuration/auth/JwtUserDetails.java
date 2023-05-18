package eu.bbmri.eric.csit.service.negotiator.configuration.auth;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtUserDetails implements NegotiatorUserDetails {

  private final Person person;

  public JwtUserDetails(
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
