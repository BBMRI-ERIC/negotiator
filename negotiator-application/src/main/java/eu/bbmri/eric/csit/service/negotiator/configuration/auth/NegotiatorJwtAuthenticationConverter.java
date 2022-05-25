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

  PersonRepository personRepository;

  public NegotiatorJwtAuthenticationConverter(PersonRepository personRepository) {
    this.personRepository = personRepository;
  }

  @Override
  public final AbstractAuthenticationToken convert(Jwt jwt) {

    Collection<GrantedAuthority> authorities = new HashSet<>();

    if (jwt.getClaims().containsKey("scope")) {
      String[] scopes = jwt.getClaims().get("scope").toString().split(" ");
      if (Arrays.asList(scopes).contains("openid")) {
        authorities.add(new SimpleGrantedAuthority("RESEARCHER"));
      }
    }
    Person person = personRepository.findByAuthName(jwt.getClaim("user_name")).orElse(null);

    String principalClaimValue = jwt.getClaimAsString("sub");

    return new NegotiatorJwtAuthenticationToken(person, jwt, authorities, principalClaimValue);
  }
}
