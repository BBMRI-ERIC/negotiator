package eu.bbmri.eric.csit.service.negotiator.configuration.security.auth;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestTemplate;

/**
 * Class to convert Oauth2 authentication using a JWT, to an internal representation of a user in
 * the Negotiator
 */
@CommonsLog
@AllArgsConstructor
public class CustomJWTAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final PersonRepository personRepository;

  private final String userInfoEndpoint;

  private final String authzClaim;

  private final String authzAdminValue;

  private final String authzResearcherValue;

  private final String authzBiobankerValue;

  // TODO: Add support for client-credentials access token, handling of errors from userinfo/get
  // claims, separate based on scopes.
  @Override
  public final AbstractAuthenticationToken convert(Jwt jwt) {
    if (jwt.hasClaim("client_id")) {
      return parseJWTAsMachineToken(jwt);
    } else {
      return parseJWTAsUserToken(jwt);
    }
  }

  private NegotiatorJwtAuthenticationToken parseJWTAsUserToken(Jwt jwt) {
    Collection<GrantedAuthority> authorities = parseUserAuthorities(jwt);
    String subjectIdentifier = jwt.getClaimAsString("sub");
    Person person =
        personRepository
            .findBySubjectId(subjectIdentifier)
            .orElseGet(() -> saveNewUserToDatabase(jwt));
    return new NegotiatorJwtAuthenticationToken(person, jwt, authorities, subjectIdentifier);
  }

  private NegotiatorJwtAuthenticationToken parseJWTAsMachineToken(Jwt jwt) {
    Collection<GrantedAuthority> authorities = getAuthoritiesFromScope(jwt);
    String clientId = jwt.getClaimAsString("client_id");
    Person person =
        personRepository.findBySubjectId(clientId).orElseGet(() -> saveNewClientAsPerson(jwt));
    return new NegotiatorJwtAuthenticationToken(person, jwt, authorities, person.getSubjectId());
  }

  private Person saveNewClientAsPerson(Jwt jwt) {
    String clientId = jwt.getClaimAsString("client_id");
    log.info(String.format("Client with id %s not in the database, adding...", clientId));
    Person person =
        Person.builder()
            .subjectId(jwt.getClaimAsString("client_id"))
            .name(jwt.getClaimAsString("client_id"))
            .email("no_email")
            .isServiceAccount(true)
            .build();
    try {
      person = personRepository.save(person);
    } catch (DataIntegrityViolationException e) {
      log.info(
          String.format("Client with id: %s already present in the db", person.getSubjectId()));
    }
    log.info(String.format("Client with id: %s added to the database", person.getSubjectId()));
    return person;
  }

  private static Collection<GrantedAuthority> getAuthoritiesFromScope(Jwt jwt) {
    Collection<GrantedAuthority> authorities = new HashSet<>();
    if (jwt.hasClaim("scope")) {
      List<String> scopes = jwt.getClaimAsStringList("scope");
      if (scopes.contains("negotiator_authz_management")) {
        authorities.add(new SimpleGrantedAuthority("ROLE_AUTHORIZATION_MANAGER"));
      }
    }
    return authorities;
  }

  /**
   * This method parses scopes/claims from the oauth server and assigns user authorities
   *
   * @param jwt the jwt token
   * @return authorities for the authenticated user
   */
  private Collection<GrantedAuthority> parseUserAuthorities(Jwt jwt) {
    Collection<GrantedAuthority> authorities = new HashSet<>();
    if (jwt.hasClaim(authzClaim)) {
      List<String> entitlements = jwt.getClaimAsStringList(authzClaim);
      if (entitlements.contains(authzAdminValue)) {
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
      }
      if (entitlements.contains(authzResearcherValue)) {
        authorities.add(new SimpleGrantedAuthority("ROLE_RESEARCHER"));
      }
      if (entitlements.contains(authzBiobankerValue)) {
        authorities.add(new SimpleGrantedAuthority("ROLE_REPRESENTATIVE"));
      }
    }
    return authorities;
  }

  private Map<String, Object> getClaims(Jwt jwt) {
    if (userInfoEndpoint != null
        && !userInfoEndpoint.isBlank()
        && jwt.getClaimAsStringList("scope").contains("openid")) {
      return getClaimsFromUserEndpoints(jwt);
    } else {
      return jwt.getClaims();
    }
  }

  private LinkedHashMap<String, Object> getClaimsFromUserEndpoints(Jwt jwt) {
    Object claims = requestClaimsFromUserInfoEndpoint(jwt);
    try {
      return (LinkedHashMap<String, Object>) claims;
    } catch (ClassCastException ex) {
      return new LinkedHashMap<>();
    }
  }

  private Object requestClaimsFromUserInfoEndpoint(Jwt jwt) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add("Authorization", String.format("Bearer %s", jwt.getTokenValue()));
    HttpEntity<String> httpEntity = new HttpEntity<>(requestHeaders);

    ResponseEntity<Object> response =
        restTemplate.exchange(this.userInfoEndpoint, HttpMethod.GET, httpEntity, Object.class);
    return response.getBody();
  }

  private Person saveNewUserToDatabase(Jwt jwt) {
    Person person =
        Person.builder()
            .subjectId(jwt.getClaimAsString("sub"))
            .name(jwt.getClaimAsString("name"))
            .email(jwt.getClaimAsString("email"))
            .build();
    try {
      personRepository.save(person);
    } catch (DataIntegrityViolationException e) {
      log.info(String.format("User with sub: %s already present in the db", person.getSubjectId()));
      return person;
    }
    log.info(String.format("User with sub: %s added to the database", person.getSubjectId()));
    return person;
  }
}
