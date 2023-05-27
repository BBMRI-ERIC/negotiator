package eu.bbmri.eric.csit.service.negotiator.configuration.auth;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
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

import java.util.*;

/**
 * Class to map the Authorities in a JWT to the ones known by the Negotiator
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


  private LinkedHashMap<String, Object> getClaimsFromUserEndpoints(Jwt jwt) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add("Authorization", String.format("Bearer %s", jwt.getTokenValue()));
    HttpEntity<String> httpEntity = new HttpEntity<>(requestHeaders);

    ResponseEntity<Object> response = restTemplate.exchange(this.userInfoEndpoint, HttpMethod.GET,
        httpEntity, Object.class);
    Object claims = response.getBody();
    try {
      return (LinkedHashMap<String, Object>) claims;
    } catch (ClassCastException ex) {
      return new LinkedHashMap<>();
    }
  }

  private Map<String, Object> getClaims(Jwt jwt) {
    if (userInfoEndpoint != null && !userInfoEndpoint.isBlank()) {
      return getClaimsFromUserEndpoints(jwt);
    } else {
      return jwt.getClaims();
    }
  }

  @Override
  public final AbstractAuthenticationToken convert(Jwt jwt) {
    Map<String, Object> claims = getClaims(jwt);

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
      }
    }
    log.debug(claims.toString());
    String principalClaimValue = jwt.getClaimAsString("sub");
    Person person;
    try {
     person = personRepository.findByAuthSubject(principalClaimValue)
              .orElseThrow(() -> new EntityNotFoundException(principalClaimValue));
    }
    catch (EntityNotFoundException e){
      log.info(String.format("User with sub %s not in the database, adding...", principalClaimValue));
      person = saveNewUserToDatabase(claims);
    }



    return new NegotiatorJwtAuthenticationToken(person, jwt, authorities, principalClaimValue);
  }

  private Person saveNewUserToDatabase(Map<String, Object> claims) {
    Person person;
    person = Person.builder()
            .authSubject(claims.get("sub").toString())
            .authName(claims.get("preferred_username").toString())
            .authEmail(claims.get("email").toString())
            .build();
    personRepository.save(person);
    log.info(String.format("User with sub: %s added to the database", person.getAuthSubject()));
    log.debug(person.toString());
    return person;
  }
}
