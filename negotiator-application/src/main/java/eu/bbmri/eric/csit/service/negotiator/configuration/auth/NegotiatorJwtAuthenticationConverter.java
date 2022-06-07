package eu.bbmri.eric.csit.service.negotiator.configuration.auth;

import eu.bbmri.eric.csit.service.negotiator.model.Person;
import eu.bbmri.eric.csit.service.negotiator.repository.PersonRepository;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class NegotiatorJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  private final String authzClaim;

  private final String authzSubjectClaim;

  private final String authzAdminValue;

  private final String authzResearcherValue;

  private final String authzBiobankerValue;

  PersonRepository personRepository;

  /**
   * Converter of JWT. It assigns Authorities based on the claims present in the JWT and enhances
   * the JWT adding the internal Person related to it.
   *
   * @param personRepository The Repository to retrieve the Person
   * @param authzClaim the name of the claim that contains the value of the claim
   * @param authzSubjectClaim the name of the claim that contains the id of the subject, used to
   *     retrieve the Person from the Repository
   * @param authzAdminValue the value of the authzClaim for Administrator role
   * @param authzResearcherValue the value of the authzClaim for Researcher role
   * @param authzBiobankerValue the value of the authzClaim for Biobanker role
   */
  public NegotiatorJwtAuthenticationConverter(
      PersonRepository personRepository,
      String authzClaim,
      String authzSubjectClaim,
      String authzAdminValue,
      String authzResearcherValue,
      String authzBiobankerValue) {
    this.personRepository = personRepository;
    this.authzClaim = authzClaim;
    this.authzAdminValue = authzAdminValue;
    this.authzResearcherValue = authzResearcherValue;
    this.authzBiobankerValue = authzBiobankerValue;
    this.authzSubjectClaim = authzSubjectClaim;
  }

  @Override
  public final AbstractAuthenticationToken convert(Jwt jwt) {

    Collection<GrantedAuthority> authorities = new HashSet<>();

    if (jwt.getClaims().containsKey(authzClaim)) {
      String[] scopes = jwt.getClaims().get(authzClaim).toString().split(" ");
      if (Arrays.asList(scopes).contains(authzAdminValue)) {
        authorities.add(new SimpleGrantedAuthority("ADMIN"));
      }
      if (Arrays.asList(scopes).contains(authzResearcherValue)) {
        authorities.add(new SimpleGrantedAuthority("RESEARCHER"));
      }
      if (Arrays.asList(scopes).contains(authzBiobankerValue)) {
        authorities.add(new SimpleGrantedAuthority("BIOBANKER"));
      }
    }
    Person person = personRepository.findByAuthName(jwt.getClaim(authzSubjectClaim)).orElse(null);

    String principalClaimValue = jwt.getClaimAsString("sub");

    return new NegotiatorJwtAuthenticationToken(person, jwt, authorities, principalClaimValue);
  }
}
