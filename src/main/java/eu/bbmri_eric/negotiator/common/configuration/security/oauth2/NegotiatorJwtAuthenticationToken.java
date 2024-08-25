package eu.bbmri_eric.negotiator.common.configuration.security.oauth2;

import eu.bbmri_eric.negotiator.common.configuration.security.NegotiatorPrincipal;
import eu.bbmri_eric.negotiator.common.configuration.security.UserPrincipal;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * A Jwt authentication token that enhances the basic one (i.e., JwtAuthenticationToken) by adding
 * UserPrincipal that is used as Principal. This allows REST endpoints to get the Principal as a
 * NegotiatorPrincipal
 */
public class NegotiatorJwtAuthenticationToken extends JwtAuthenticationToken {

  private final NegotiatorPrincipal principal;

  public NegotiatorJwtAuthenticationToken(
      Person person, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
    super(jwt, authorities);
    this.principal = new UserPrincipal(person);
  }

  @Override
  public NegotiatorPrincipal getPrincipal() {
    return principal;
  }
}
