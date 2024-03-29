package eu.bbmri_eric.negotiator.configuration.security.auth;

import eu.bbmri_eric.negotiator.database.model.Person;
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
      Person person, Jwt jwt, Collection<? extends GrantedAuthority> authorities, String name) {
    super(jwt, authorities, name);
    this.userDetails = new JwtUserDetails(person, authorities);
  }

  @Override
  public NegotiatorUserDetails getPrincipal() {
    return userDetails;
  }
}
