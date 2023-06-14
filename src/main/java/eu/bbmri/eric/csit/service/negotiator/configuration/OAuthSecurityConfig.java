package eu.bbmri.eric.csit.service.negotiator.configuration;

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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class OAuthSecurityConfig {

  @Autowired
  public NegotiatorUserDetailsService userDetailsService;

  @Autowired
  public PersonRepository personRepository;

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
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .cors()
        .disable()
        .csrf()
        .disable()
        .headers()
        .frameOptions()
        .disable();

    http
        .authorizeHttpRequests().antMatchers("/v3/negotiations/**").authenticated()
        .and()
        .authorizeHttpRequests().antMatchers(HttpMethod.GET, "/v3/access-criteria/**").permitAll()
        .and()
        .authorizeHttpRequests().antMatchers(HttpMethod.POST, "/v3/access-criteria/**")
        .hasRole("REPRESENTATIVE")
        .and()
        .authorizeHttpRequests()
        .antMatchers(HttpMethod.POST, "/directory/create_query", "/v3/requests/**").permitAll()
        .and()
        .authorizeHttpRequests()
        .antMatchers(HttpMethod.PUT, "/directory/create_query", "/v3/requests/**").permitAll()
        .and()
        .authorizeHttpRequests()
        .antMatchers(HttpMethod.POST, "/v3/data-sources/**")
        .hasAuthority("ADMIN")
        .and()
        .authorizeHttpRequests()
        .antMatchers(HttpMethod.PUT, "/v3/data-sources/**")
        .hasAuthority("ADMIN")
        .and()
        .authorizeHttpRequests()
        .antMatchers("/v3/projects/**")
        .hasAuthority("RESEARCHER")
        .anyRequest()
        .permitAll()
        .and()
        .httpBasic();

    if (!jwtIssuer.isEmpty()) {
      http.oauth2ResourceServer()
          .jwt()
          .jwtAuthenticationConverter(
              new JwtAuthenticationConverter(
                  personRepository,
                  userInfoEndpoint,
                  authzClaim,
                  authzSubjectClaim,
                  authzAdminValue,
                  authzResearcherValue,
                  authzBiobankerValue,
                  authzResourceIdPrefixClaim));
    }
    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withJwkSetUri(this.jwksUrl)
        .jwtProcessorCustomizer(customizer -> {
          customizer.setJWSTypeVerifier(
              new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType(jwtType)));
        })
        .build();
  }
}
