package eu.bbmri_eric.negotiator.common.configuration.security.auth;

import eu.bbmri_eric.negotiator.user.JwtUserDetails;
import eu.bbmri_eric.negotiator.user.NegotiatorUserDetails;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * A Jwt authentication token that enhances the basic one (i.e., JwtAuthenticationToken) by adding
 * JwtUserDetails that is used as Principal. This allows REST endpoints to get the Principal as a
 * NegotiatorUserDetails
 */
public class NegotiatorJwtAuthenticationToken extends JwtAuthenticationToken {

  private final JwtUserDetails userDetails;

  public NegotiatorJwtAuthenticationToken(
      Person person, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
    super(jwt, authorities);
    this.userDetails = new JwtUserDetails(person, authorities);
  }

  @Override
  public NegotiatorUserDetails getPrincipal() {
    return userDetails;
  }
}
