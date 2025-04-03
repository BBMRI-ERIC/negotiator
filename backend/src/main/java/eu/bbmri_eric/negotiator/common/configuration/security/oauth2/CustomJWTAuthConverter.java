package eu.bbmri_eric.negotiator.common.configuration.security.oauth2;

import eu.bbmri_eric.negotiator.common.exceptions.WrongJWTException;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@CommonsLog
@AllArgsConstructor
public class CustomJWTAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  // Constants for claim names and scopes
  private static final String CLAIM_CLIENT_ID = "client_id";
  private static final String CLAIM_SCOPE = "scope";
  private static final String CLAIM_SUB = "sub";
  private static final String CLAIM_NAME = "name";
  private static final String CLAIM_EMAIL = "email";
  private static final String SCOPE_OPENID = "openid";
  private static final String NEGOTIATOR_AUTHZ_MANAGEMENT = "negotiator_authz_management";
  private static final String NEGOTIATOR_RESOURCE_MANAGEMENT = "negotiator_resource_management";
  private static final String NEGOTIATOR_MONITORING = "negotiator_monitoring";

  private final PersonRepository personRepository;
  private final String userInfoEndpoint;
  private final String authzClaim;
  private final String authzAdminValue;
  private final String authzResearcherValue;
  private final String authzBiobankerValue;

  // Cache for user info claims
  private static final Map<String, Map<String, Object>> userInfoCache = new ConcurrentHashMap<>();

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    if (isClientCredentialsToken(jwt)) {
      return parseJWTAsMachineToken(jwt);
    } else {
      return parseJWTAsUserToken(jwt);
    }
  }

  private static boolean isClientCredentialsToken(Jwt jwt) {
    return jwt.hasClaim(CLAIM_CLIENT_ID)
        && !jwt.getClaimAsString(CLAIM_SCOPE).contains(SCOPE_OPENID);
  }

  private NegotiatorJwtAuthenticationToken parseJWTAsUserToken(Jwt jwt) {
    String subjectIdentifier = jwt.getClaimAsString(CLAIM_SUB);
    Map<String, Object> claims = getClaims(jwt);
    Person person =
        personRepository
            .findBySubjectId(subjectIdentifier)
            .map(existing -> updatePersonIfNecessary(existing, claims))
            .orElseGet(() -> saveNewUserAsPerson(claims));
    Collection<GrantedAuthority> authorities = parseUserAuthorities(claims);
    return new NegotiatorJwtAuthenticationToken(person, jwt, authorities);
  }

  private NegotiatorJwtAuthenticationToken parseJWTAsMachineToken(Jwt jwt) {
    String clientId = jwt.getClaimAsString(CLAIM_CLIENT_ID);
    Person person =
        personRepository.findBySubjectId(clientId).orElseGet(() -> saveNewClientAsPerson(jwt));
    Collection<GrantedAuthority> authorities = getAuthoritiesFromScope(jwt);
    return new NegotiatorJwtAuthenticationToken(person, jwt, authorities);
  }

  private Person saveNewClientAsPerson(Jwt jwt) {
    String clientId = jwt.getClaimAsString(CLAIM_CLIENT_ID);
    log.info("Client with id %s not in the database, adding...".formatted(clientId));
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
      log.info("Client with id: %s already present in the db".formatted(person.getSubjectId()));
    }
    log.info("Client with id: %s added to the database".formatted(person.getSubjectId()));
    return person;
  }

  private static Collection<GrantedAuthority> getAuthoritiesFromScope(Jwt jwt) {
    Set<GrantedAuthority> authorities = new HashSet<>();
    if (jwt.hasClaim(CLAIM_SCOPE)) {
      String scopes = jwt.getClaimAsString(CLAIM_SCOPE);
      if (scopes.contains(NEGOTIATOR_AUTHZ_MANAGEMENT)) {
        authorities.add(new SimpleGrantedAuthority("ROLE_AUTHORIZATION_MANAGER"));
      }
      if (scopes.contains(NEGOTIATOR_RESOURCE_MANAGEMENT)) {
        authorities.add(new SimpleGrantedAuthority("ROLE_RESOURCE_MANAGER"));
      }
      if (scopes.contains(NEGOTIATOR_MONITORING)) {
        authorities.add(new SimpleGrantedAuthority("ROLE_PROMETHEUS"));
      }
    }
    return authorities;
  }

  private Collection<GrantedAuthority> parseUserAuthorities(Map<String, Object> claims) {
    Set<GrantedAuthority> authorities = new HashSet<>();
    if (claims.containsKey(authzClaim)) {
      @SuppressWarnings("unchecked")
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
    String subject = jwt.getSubject();
    if (subject != null && userInfoCache.containsKey(subject)) {
      return userInfoCache.get(subject);
    }
    if (shouldFetchUserInfo(jwt)) {
      return fetchAndCacheUserInfo(jwt);
    }
    return jwt.getClaims();
  }

  private boolean shouldFetchUserInfo(Jwt jwt) {
    return userInfoEndpoint != null
        && !userInfoEndpoint.isBlank()
        && jwt.getClaimAsString(CLAIM_SCOPE).contains(SCOPE_OPENID);
  }

  private Map<String, Object> fetchAndCacheUserInfo(Jwt jwt) {
    Object claimsResponse = requestClaimsFromUserInfoEndpoint(jwt);
    if (claimsResponse instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> claims = (Map<String, Object>) claimsResponse;
      userInfoCache.put(jwt.getSubject(), claims);
      log.info("USER_LOGIN: User %s logged in.".formatted(claims.get(CLAIM_NAME)));
      return claims;
    } else {
      log.warn("Unexpected response format from user info endpoint. Falling back to JWT claims.");
      return jwt.getClaims();
    }
  }

  private Object requestClaimsFromUserInfoEndpoint(Jwt jwt) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwt.getTokenValue());
    HttpEntity<String> entity = new HttpEntity<>(headers);
    try {
      ResponseEntity<Object> response =
          restTemplate.exchange(userInfoEndpoint, HttpMethod.GET, entity, Object.class);
      return response.getBody();
    } catch (RestClientException e) {
      log.error("Could not connect to user info endpoint: %s".formatted(e.getMessage()), e);
      return Collections.emptyMap();
    }
  }

  private Person saveNewUserAsPerson(Map<String, Object> userInfo) {
    try {
      Person person =
          Person.builder()
              .subjectId(Objects.toString(userInfo.get(CLAIM_SUB), null))
              .name(Objects.toString(userInfo.get(CLAIM_NAME), "Unknown"))
              .email(Objects.toString(userInfo.get(CLAIM_EMAIL), "no_email"))
              .build();
      personRepository.save(person);
      log.info("User with sub: %s added to the database".formatted(person.getSubjectId()));
      return person;
    } catch (ConstraintViolationException | NullPointerException e) {
      log.error("Could not create user from claims: " + userInfo, e);
      throw new WrongJWTException();
    } catch (DataIntegrityViolationException e) {
      log.info("User with sub: %s already present in the db".formatted(userInfo.get(CLAIM_SUB)));
      return personRepository
          .findBySubjectId(Objects.toString(userInfo.get(CLAIM_SUB), ""))
          .orElseThrow(
              () -> new AuthenticationCredentialsNotFoundException("Could not link user to token"));
    }
  }

  private Person updatePersonIfNecessary(Person person, Map<String, Object> userInfo) {
    boolean updated = false;
    String newName = Objects.toString(userInfo.get(CLAIM_NAME), person.getName());
    String newEmail = Objects.toString(userInfo.get(CLAIM_EMAIL), person.getEmail());

    if (!person.getName().equals(newName)) {
      person.setName(newName);
      updated = true;
    }
    if (!person.getEmail().equals(newEmail)) {
      person.setEmail(newEmail);
      updated = true;
    }
    if (updated) {
      personRepository.save(person);
      log.info("User with sub: %s updated in the database".formatted(person.getSubjectId()));
    }
    return person;
  }

  public static void cleanCache() {
    log.debug("Clearing userInfo cache.");
    userInfoCache.clear();
  }
}
