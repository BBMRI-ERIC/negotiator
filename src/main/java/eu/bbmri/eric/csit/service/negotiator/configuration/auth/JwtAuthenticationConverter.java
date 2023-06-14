package eu.bbmri.eric.csit.service.negotiator.configuration.auth;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
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
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final PersonRepository personRepository;

  private final String userInfoEndpoint;

  private final String authzClaim;

  private final String authzSubjectClaim;

  private final String authzAdminValue;

  private final String authzResearcherValue;

  private final String authzBiobankerValue;

  private final String authzResourceClaimPrefix;

  private Collection<GrantedAuthority> addAuthoritiesForIndividualResources(
      List<String> scopes) {
    Collection<GrantedAuthority> authorities = new HashSet<>();
    for (String scope : scopes) {
      // TODO: try to generalize the transformation
      if (scope.contains(authzResourceClaimPrefix)) {
        String resourceId = StringUtils.substringBetween(scope, authzResourceClaimPrefix, "#perun")
            .replace(".", ":");
        authorities.add(new SimpleGrantedAuthority(resourceId));
      }
    }
    return authorities;
  }

  @Override
  public final AbstractAuthenticationToken convert(Jwt jwt) {
    Map<String, Object> claims = getClaims(jwt);
    log.debug(claims);
    Collection<GrantedAuthority> authorities = assignAuthorities(claims);
    String subjectIdentifier = jwt.getClaimAsString("sub");
    Person person;
    try {
      person = personRepository.findByAuthSubject(subjectIdentifier)
          .orElseThrow(() -> new EntityNotFoundException(subjectIdentifier));
    } catch (EntityNotFoundException e) {
      log.info(String.format("User with sub %s not in the database, adding...", subjectIdentifier));
      person = saveNewUserToDatabase(claims);
    }

    return new NegotiatorJwtAuthenticationToken(person, jwt, authorities, subjectIdentifier);
  }

  /**
   * This method parses scopes/claims from the oauth server and assigns user authorities
   *
   * @param claims Claims from the oauth authorization provider
   * @return authorities for the authenticated user
   */
  private Collection<GrantedAuthority> assignAuthorities(Map<String, Object> claims) {
    Collection<GrantedAuthority> authorities = new HashSet<>();
    if (claims.containsKey(authzClaim)) {
      List<String> scopes = (List<String>) claims.get(authzClaim);
      if (scopes.contains(authzAdminValue)) {
        authorities.add(new SimpleGrantedAuthority("ADMIN"));
      }
      if (scopes.contains(authzResearcherValue)) {
        authorities.add(new SimpleGrantedAuthority("RESEARCHER"));
      }
      if (scopes.contains(authzBiobankerValue)) {
        authorities.add(new SimpleGrantedAuthority("BIOBANKER"));
        authorities.addAll(addAuthoritiesForIndividualResources(scopes));
      }
    }
    return authorities;
  }

  private Map<String, Object> getClaims(Jwt jwt) {
    if (userInfoEndpoint != null && !userInfoEndpoint.isBlank()) {
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

    ResponseEntity<Object> response = restTemplate.exchange(this.userInfoEndpoint, HttpMethod.GET,
        httpEntity, Object.class);
    return response.getBody();
  }

  private Person saveNewUserToDatabase(Map<String, Object> claims) {
    Person person;
    person = Person.builder()
        .authSubject(String.valueOf(claims.get("sub")))
        .authName(String.valueOf(claims.get("preferred_username")))
        .authEmail(String.valueOf(claims.get("email").toString()))
        .build();
    personRepository.save(person);
    log.info(String.format("User with sub: %s added to the database", person.getAuthSubject()));
    log.debug(person);
    return person;
  }
}
