package eu.bbmri_eric.negotiator.user;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public class JwtUserDetails implements NegotiatorUserDetails {

  private final Person person;

  public JwtUserDetails(Person person, Collection<? extends GrantedAuthority> authorities) {
    this.person = person;
  }

  public Object getPrincipal() {
    return getPerson();
  }

  public Person getPerson() {
    return person;
  }
}