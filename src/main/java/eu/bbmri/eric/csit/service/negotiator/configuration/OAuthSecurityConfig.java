package eu.bbmri.eric.csit.service.negotiator.configuration;

import static org.springframework.security.config.http.MatcherType.mvc;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import eu.bbmri.eric.csit.service.negotiator.configuration.auth.JwtAuthenticationConverter;
import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetailsService;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
public class OAuthSecurityConfig {

  @Autowired public NegotiatorUserDetailsService userDetailsService;

  @Autowired public PersonRepository personRepository;

  @Value("${spring.security.oauth2.resourceserver.jwt.user-info-uri}")
  private String userInfoEndpoint;

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

  @Bean
  MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
    return new MvcRequestMatcher.Builder(introspector);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(Customizer.withDefaults())
        .authorizeHttpRequests(
            authz ->
                authz
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
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/directory/create_query"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/v3/requests"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.PUT, "/directory/create_query"))
                    .authenticated()
                    .requestMatchers(mvc.pattern(HttpMethod.PUT, "/v3/requests/**"))
                    .authenticated()
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/v3/data-sources/**"))
                    .hasAuthority("ADMIN")
                    .requestMatchers(mvc.pattern(HttpMethod.PUT, "/v3/data-sources/**"))
                    .hasAuthority("ADMIN")
                    .requestMatchers(mvc.pattern("/v3/users/**"))
                    .authenticated()
                    .requestMatchers(mvc.pattern("/v3/projects/**"))
                    .hasRole("RESEARCHER")
                    .anyRequest()
                    .permitAll());

    if (!jwtIssuer.isEmpty()) {
      http.oauth2ResourceServer(
          oauth ->
              oauth.jwt(
                  jwt ->
                      jwt.jwtAuthenticationConverter(
                          new JwtAuthenticationConverter(
                              personRepository,
                              userInfoEndpoint,
                              authzClaim,
                              authzSubjectClaim,
                              authzAdminValue,
                              authzResearcherValue,
                              authzBiobankerValue,
                              authzResourceIdPrefixClaim))));
    }
    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withJwkSetUri(this.jwksUrl)
        .jwtProcessorCustomizer(
            customizer -> {
              customizer.setJWSTypeVerifier(
                  new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType(jwtType)));
            })
        .build();
  }
}
