package eu.bbmri_eric.negotiator.common.configuration.security.oauth2;

import eu.bbmri_eric.negotiator.common.exceptions.WrongJWTException;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.validation.ConstraintViolationException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import org.springframework.web.client.RestClientException;
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

  private static final Map<String, LinkedHashMap<String, Object>> userInfoCache =
      new ConcurrentHashMap<>();

  @Override
  public final AbstractAuthenticationToken convert(Jwt jwt) {
    if (isClientCredentialsToken(jwt)) {
      return parseJWTAsMachineToken(jwt);
    } else {
      return parseJWTAsUserToken(jwt);
    }
  }

  private static boolean isClientCredentialsToken(Jwt jwt) {
    return jwt.hasClaim("client_id") && !jwt.getClaimAsString("scope").contains("openid");
  }

  private NegotiatorJwtAuthenticationToken parseJWTAsUserToken(Jwt jwt) {
    String subjectIdentifier = jwt.getClaimAsString("sub");
    Map<String, Object> userInfo = getClaims(jwt);
    Person person =
        personRepository
            .findBySubjectId(subjectIdentifier)
            .map(existingPerson -> updatePersonIfNecessary(existingPerson, userInfo))
            .orElseGet(() -> saveNewUserAsPerson(userInfo));

    return new NegotiatorJwtAuthenticationToken(person, jwt, parseUserAuthorities(userInfo));
  }

  private NegotiatorJwtAuthenticationToken parseJWTAsMachineToken(Jwt jwt) {
    String clientId = jwt.getClaimAsString("client_id");
    Person person =
        personRepository.findBySubjectId(clientId).orElseGet(() -> saveNewClientAsPerson(jwt));
    return new NegotiatorJwtAuthenticationToken(person, jwt, getAuthoritiesFromScope(jwt));
  }

  private Person saveNewClientAsPerson(Jwt jwt) {
    String clientId = jwt.getClaimAsString("client_id");
    log.info(String.format("Client with id %s not in the database, adding...", clientId));
    Person person =
        Person.builder()
            .subjectId(clientId)
            .name(clientId)
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
      String scopes = jwt.getClaimAsString("scope");
      if (scopes.contains("negotiator_authz_management")) {
        authorities.add(new SimpleGrantedAuthority("ROLE_AUTHORIZATION_MANAGER"));
      }
      if (scopes.contains("negotiator_resource_management")) {
        authorities.add(new SimpleGrantedAuthority("ROLE_RESOURCE_MANAGER"));
      }
      if (scopes.contains("negotiator_monitoring")) {
        authorities.add(new SimpleGrantedAuthority("ROLE_PROMETHEUS"));
      }
    }
    return authorities;
  }

  /**
   * This method parses scopes/claims from the oauth server and assigns user authorities
   *
   * @param claims map of claims from the oauth server
   * @return authorities for the authenticated user
   */
  private Collection<GrantedAuthority> parseUserAuthorities(Map<String, Object> claims) {
    Collection<GrantedAuthority> authorities = new HashSet<>();
    if (claims.containsKey(authzClaim)) {
      List<String> entitlements = (List<String>) claims.get(authzClaim);
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
    if (userInfoCache.containsKey(jwt.getSubject())) {
      return userInfoCache.get(jwt.getSubject());
    }
    if (userInfoEndpoint != null
        && !userInfoEndpoint.isBlank()
        && jwt.getClaimAsString("scope").contains("openid")) {
      return getClaimsFromUserEndpoints(jwt);
    } else {
      return jwt.getClaims();
    }
  }

  private LinkedHashMap<String, Object> getClaimsFromUserEndpoints(Jwt jwt) {
    Object claims = requestClaimsFromUserInfoEndpoint(jwt);
    LinkedHashMap<String, Object> mappedClaims;
    try {
      mappedClaims = (LinkedHashMap<String, Object>) claims;
    } catch (ClassCastException ex) {
      return new LinkedHashMap<>();
    }
    userInfoCache.put(jwt.getSubject(), mappedClaims);
    log.info("USER_LOGIN: User %s logged in.".formatted(mappedClaims.get("name")));
    return mappedClaims;
  }

  private Object requestClaimsFromUserInfoEndpoint(Jwt jwt) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add("Authorization", String.format("Bearer %s", jwt.getTokenValue()));
    HttpEntity<String> httpEntity = new HttpEntity<>(requestHeaders);
    ResponseEntity<Object> response = null;
    try {
      response =
          restTemplate.exchange(this.userInfoEndpoint, HttpMethod.GET, httpEntity, Object.class);
    } catch (RestClientException e) {
      log.error("Could not connect to user info endpoint");
      log.error(e.getMessage());
      return new Object();
    }
    return response.getBody();
  }

  private Person saveNewUserAsPerson(Map<String, Object> userInfo) {
    Person person;
    try {
      person =
          Person.builder()
              .subjectId(userInfo.get("sub").toString())
              .name(userInfo.get("name").toString())
              .email(userInfo.get("email").toString())
              .build();
    } catch (ConstraintViolationException | NullPointerException e) {
      log.error("Could not create user from claims: " + userInfo.toString());
      throw new WrongJWTException();
    }
    try {
      personRepository.save(person);
    } catch (DataIntegrityViolationException e) {
      log.info(String.format("User with sub: %s already present in the db", person.getSubjectId()));
      return person;
    }
    log.info(String.format("User with sub: %s added to the database", person.getSubjectId()));
    return person;
  }

  private Person updatePersonIfNecessary(Person person, Map<String, Object> userInfo) {
    boolean isUpdated = false;

    if (!person.getName().equals(userInfo.get("name").toString())) {
      person.setName(userInfo.get("name").toString());
      isUpdated = true;
    }
    if (!person.getEmail().equals(userInfo.get("email").toString())) {
      person.setEmail(userInfo.get("email").toString());
      isUpdated = true;
    }

    if (isUpdated) {
      personRepository.save(person);
      log.info(String.format("User with sub: %s updated in the database", person.getSubjectId()));
    }

    return person;
  }

  static void cleanCache() {
    log.debug("Clearing userInfo cache.");
    userInfoCache.clear();
  }
}
