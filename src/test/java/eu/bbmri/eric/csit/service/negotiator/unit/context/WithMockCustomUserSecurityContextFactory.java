package eu.bbmri.eric.csit.service.negotiator.unit.context;

import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorJwtAuthenticationToken;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * SecurityContext to be used in unit tests. It creates a mock NegotiatorJwtAuthenticationToken
 * with Person, authorities and roles taken from the WithMockNegotiationUser.
 */
public class WithMockCustomUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockNegotiatorUser> {
  @Override
  public SecurityContext createSecurityContext(WithMockNegotiatorUser customUser) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();

    Person principal =
        Person.builder()
            .id(customUser.id())
            .authName(customUser.authName())
            .authSubject(customUser.authSubject())
            .authEmail(customUser.authEmail())
            .build();

    Collection<GrantedAuthority> authorities =
        Arrays.stream(customUser.authorities())
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

    // No need to put truthful information in the JWT since they are not needed at this unit tests level
    HashMap<String, Object> headers = new HashMap<>();
    headers.put("typ", "JWT");
    HashMap<String, Object> claims = new HashMap<>();
    claims.put("sub", customUser.authSubject());
    Jwt jwt = new Jwt("testToken", Instant.now(), Instant.now(), headers, claims);

    Authentication auth =
        new NegotiatorJwtAuthenticationToken(principal, jwt, authorities, customUser.authSubject());

    context.setAuthentication(auth);
    return context;
  }
}