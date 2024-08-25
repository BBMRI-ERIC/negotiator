package eu.bbmri_eric.negotiator.common.configuration.security.oauth2;

import eu.bbmri_eric.negotiator.common.configuration.security.auth.CustomJWTAuthConverter;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Configuration
public class OAuth2Configuration {

  private final PersonRepository personRepository;

  private final JwtDecoder jwtDecoder;

  @Value("${spring.security.oauth2.resourceserver.jwt.user-info-uri}")
  private String userInfoEndpoint;

  @Value("${negotiator.authorization.claim}")
  private String authzClaim;

  @Value("${negotiator.authorization.admin-claim-value}")
  private String authzAdminValue;

  @Value("${negotiator.authorization.researcher-claim-value}")
  private String authzResearcherValue;

  @Value("${negotiator.authorization.biobanker-claim-value}")
  private String authzBiobankerValue;

  public OAuth2Configuration(PersonRepository personRepository, JwtDecoder jwtDecoder) {
    this.personRepository = personRepository;
    this.jwtDecoder = jwtDecoder;
  }

  public void configure(OAuth2ResourceServerConfigurer<HttpSecurity> oauth) {
    oauth.jwt(
        jwt ->
            jwt.jwtAuthenticationConverter(
                    new CustomJWTAuthConverter(
                        personRepository,
                        userInfoEndpoint,
                        authzClaim,
                        authzAdminValue,
                        authzResearcherValue,
                        authzBiobankerValue))
                .decoder(jwtDecoder));
  }
}
