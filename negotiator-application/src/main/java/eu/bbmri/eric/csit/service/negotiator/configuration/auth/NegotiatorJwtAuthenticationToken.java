package eu.bbmri.eric.csit.service.negotiator.configuration.auth;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class NegotiatorJwtAuthenticationToken extends JwtAuthenticationToken {

  private final NegotiatorJwtUserDetails userDetails;

  public NegotiatorJwtAuthenticationToken(
      Person person, Jwt jwt, Collection<? extends GrantedAuthority> authorities, String name) {
    super(jwt, authorities, name);
    this.userDetails = new NegotiatorJwtUserDetails(person, authorities);
  }

  @Override
  public NegotiatorUserDetails getPrincipal() {
    return userDetails;
  }
}
