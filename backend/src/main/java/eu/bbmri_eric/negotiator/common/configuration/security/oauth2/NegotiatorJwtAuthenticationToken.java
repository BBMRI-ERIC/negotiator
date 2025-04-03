package eu.bbmri_eric.negotiator.common.configuration.security.oauth2;

import eu.bbmri_eric.negotiator.common.NegotiatorPrincipal;
import eu.bbmri_eric.negotiator.common.UserPrincipal;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.Collection;
import java.util.Objects;
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

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    NegotiatorJwtAuthenticationToken that = (NegotiatorJwtAuthenticationToken) o;
    return Objects.equals(principal, that.principal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), principal);
  }
}
