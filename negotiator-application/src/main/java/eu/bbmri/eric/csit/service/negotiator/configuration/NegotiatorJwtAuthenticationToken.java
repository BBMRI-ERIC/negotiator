package eu.bbmri.eric.csit.service.negotiator.configuration;

import eu.bbmri.eric.csit.service.negotiator.model.Person;
import eu.bbmri.eric.csit.service.negotiator.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.repository.RequestRepository;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class NegotiatorJwtAuthenticationToken extends JwtAuthenticationToken {

  @Autowired private PersonRepository personRepository;
  @Autowired private RequestRepository requestRepository;

  private Jwt jwt;
  private Person person;

  public NegotiatorJwtAuthenticationToken(Jwt jwt) {
    super(jwt);
  }

  public NegotiatorJwtAuthenticationToken(
      Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
    super(jwt, authorities);
  }

  public NegotiatorJwtAuthenticationToken(
      Jwt jwt, Collection<? extends GrantedAuthority> authorities, String name, Person person) {
    super(jwt, authorities, name);
    this.jwt = jwt;
    this.person = person;
  }

  public Person getPerson() {
    return person;
  }
}
