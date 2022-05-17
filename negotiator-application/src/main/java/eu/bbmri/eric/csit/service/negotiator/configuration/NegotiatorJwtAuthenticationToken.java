package eu.bbmri.eric.csit.service.negotiator.configuration;

import eu.bbmri.eric.csit.service.negotiator.model.Person;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class NegotiatorJwtAuthenticationToken extends JwtAuthenticationToken {

  private final Person person;

  public NegotiatorJwtAuthenticationToken(Jwt jwt, Person person) {
    super(jwt);
    this.person = person;
  }

  public NegotiatorJwtAuthenticationToken(
      Jwt jwt, Collection<? extends GrantedAuthority> authorities, Person person) {
    super(jwt, authorities);
    this.person = person;
  }

  public NegotiatorJwtAuthenticationToken(
      Jwt jwt, Collection<? extends GrantedAuthority> authorities, String name, Person person) {
    super(jwt, authorities, name);
    this.person = person;
  }

  @Override
  public Person getPrincipal() {
    return person;
  }
}
