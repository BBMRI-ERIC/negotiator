package eu.bbmri_eric.negotiator.configuration.security;

import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.AUD;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import eu.bbmri_eric.negotiator.configuration.ExceptionHandlerFilter;
import eu.bbmri_eric.negotiator.configuration.security.auth.CustomJWTAuthConverter;
import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import java.util.List;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
public class OAuthSecurityConfig {

  @Autowired public NegotiatorUserDetailsService userDetailsService;

  @Autowired public PersonRepository personRepository;

  @Value("${spring.security.oauth2.resourceserver.jwt.user-info-uri}")
  private String userInfoEndpoint;

  @Value("${spring.security.oauth2.resourceserver.opaquetoken.introspection-uri}")
  private String introspectionEndpoint;

  @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}")
  private String clientId;

  @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}")
  private String clientSecret;

  @Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
  private String audience;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String jwtIssuer;

  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwksUrl;

  @Value("${spring.security.oauth2.resourceserver.jwt.type}")
  private String jwtType;

  @Value("${negotiator.authorization.claim}")
  private String authzClaim;

  @Value("${negotiator.authorization.subject-claim}")
  private String authzSubjectClaim;

  @Value("${negotiator.authorization.admin-claim-value}")
  private String authzAdminValue;

  @Value("${negotiator.authorization.researcher-claim-value}")
  private String authzResearcherValue;

  @Value("${negotiator.authorization.biobanker-claim-value}")
  private String authzBiobankerValue;

  @Value("${negotiator.authorization.resource-claim-prefix}")
  private String authzResourceIdPrefixClaim;

  @Value("${spring.security.csrf.enabled:true}")
  private boolean csrfEnabled;

  @Value("${management.endpoint.prometheus.ip}")
  private String prometheusWhitelistedIp;

  @Autowired ExceptionHandlerFilter exceptionHandlerFilter;

  @Bean
  MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
    return new MvcRequestMatcher.Builder(introspector);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc)
      throws Exception {
    http.addFilterBefore(exceptionHandlerFilter, BearerTokenAuthenticationFilter.class)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(Customizer.withDefaults())
        .authorizeHttpRequests(
            authz ->
                authz
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/negotiations"))
                    .hasRole("ADMIN")
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/negotiations/lifecycle"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/v3/negotiations/*/attachments/*"))
                    .authenticated()
                    .requestMatchers(mvc.pattern("/v3/attachments/**"))
                    .authenticated()
                    .requestMatchers(mvc.pattern("/v3/negotiations/**"))
                    .authenticated()
                    .requestMatchers(mvc.pattern("/v3/access-criteria"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/access-forms/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/directory/create_query"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/v3/requests"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.PUT, "/directory/create_query"))
                    .authenticated()
                    .requestMatchers(mvc.pattern(HttpMethod.PUT, "/v3/requests/**"))
                    .authenticated()
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/v3/discovery-services/**"))
                    .hasAuthority("ADMIN")
                    .requestMatchers(mvc.pattern(HttpMethod.PUT, "/v3/discovery-services/**"))
                    .hasAuthority("ADMIN")
                    .requestMatchers(mvc.pattern("/v3/users"))
                    .authenticated()
                    .requestMatchers(mvc.pattern("/v3/userinfo"))
                    .authenticated()
                    .requestMatchers(mvc.pattern("/v3/users/roles"))
                    .authenticated()
                    .requestMatchers(mvc.pattern("/v3/users/resources"))
                    .authenticated()
                    .requestMatchers(mvc.pattern("/v3/users/*/resources"))
                    .hasRole("AUTHORIZATION_MANAGER")
                    .requestMatchers(mvc.pattern("/v3/users/*/negotiations"))
                    .authenticated()
                    .requestMatchers(mvc.pattern("/actuator/prometheus"))
                    // Needs to be IPv6 address
                    .access(
                        new WebExpressionAuthorizationManager(
                            "hasIpAddress('%s')".formatted(prometheusWhitelistedIp)))
                    .anyRequest()
                    .permitAll());

    if (!jwtIssuer.isEmpty()) {
      http.oauth2ResourceServer(
          oauth ->
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
                          .decoder(jwtDecoder())));
    }
    if (!csrfEnabled) {
      http.csrf(AbstractHttpConfigurer::disable);
    }
    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = setupJWTDecoder();
    addTokenValidators(decoder);
    return decoder;
  }

  @NonNull
  private NimbusJwtDecoder setupJWTDecoder() {
    return NimbusJwtDecoder.withJwkSetUri(this.jwksUrl)
        .jwtProcessorCustomizer(
            customizer -> {
              customizer.setJWSTypeVerifier(
                  new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType(jwtType)));
            })
        .build();
  }

  private void addTokenValidators(NimbusJwtDecoder decoder) {
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            introspectionValidator(), new JwtIssuerValidator(jwtIssuer), audienceValidator()));
  }

  @Bean
  public OAuth2TokenValidator<Jwt> introspectionValidator() {
    return new IntrospectionValidator(introspectionEndpoint, clientId, clientSecret);
  }

  OAuth2TokenValidator<Jwt> audienceValidator() {
    return new JwtClaimValidator<List<String>>(AUD, aud -> aud.contains(audience));
  }
}
