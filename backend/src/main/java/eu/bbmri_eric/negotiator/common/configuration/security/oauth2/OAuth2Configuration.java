package eu.bbmri_eric.negotiator.common.configuration.security.oauth2;

import eu.bbmri_eric.negotiator.user.PersonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Class for OAuth2 and OIDC configuration. */
@Configuration
public class OAuth2Configuration {

  private final PersonRepository personRepository;

  private final JwtDecoder jwtDecoder;

  private final AuthenticationEntryPoint authenticationEntryPoint;

  @Value("${spring.security.oauth2.resourceserver.jwt.user-info-uri}")
  private String userInfoEndpoint;

  @Value("${negotiator.authorization.claim}")
  private String authzClaim;

  @Value("${negotiator.authorization.admin-claim-value}")
  private String authzAdminValue;

  public OAuth2Configuration(
      PersonRepository personRepository,
      JwtDecoder jwtDecoder,
      AuthenticationEntryPoint authenticationEntryPoint) {
    this.personRepository = personRepository;
    this.jwtDecoder = jwtDecoder;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  public void configure(OAuth2ResourceServerConfigurer<HttpSecurity> oauth) {
    oauth.authenticationEntryPoint(authenticationEntryPoint);
    oauth.jwt(
        jwt ->
            jwt.jwtAuthenticationConverter(
                    new CustomJWTAuthConverter(
                        personRepository, userInfoEndpoint, authzClaim, authzAdminValue))
                .decoder(jwtDecoder));
  }
}
