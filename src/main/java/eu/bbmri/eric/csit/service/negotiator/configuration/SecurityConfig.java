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
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@Profile({"dev", "prod"})
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired
  public NegotiatorUserDetailsService userDetailsService;

  @Autowired
  public DataSource dataSource;

  @Autowired
  public PasswordEncoder passwordEncoder;

  @Autowired
  public PersonRepository personRepository;

  @Value("${spring.security.oauth2.resourceserver.jwt.user-info-uri}")
  private String userInfoEndpoint;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String jwtIssuer;

  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwksUrl;

  @Value("${spring.security.oauth2.resourceserver.jwt.type}")
  private String jwkType;

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

  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withJwkSetUri(this.jwksUrl)
        .jwtProcessorCustomizer(customizer -> {
          customizer.setJWSTypeVerifier(
              new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType(jwkType)));
        })
        .build();
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(userDetailsService)
        .passwordEncoder(passwordEncoder)
        .and()
        .jdbcAuthentication()
        .dataSource(dataSource);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {

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

    http.authorizeRequests()
        .antMatchers(HttpMethod.POST, "/v3/data-sources/*")
        .hasAuthority("ADMIN")
        .and()
        .authorizeRequests()
        .antMatchers("/v3/projects/**", "/v3/negotiations/**", "/v3/access-criteria/**")
        .hasAuthority("RESEARCHER")
        .antMatchers("/directory/create_query")
        .hasAuthority("EXT_SERV")
        .and()
        .authorizeRequests()
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
                  authzBiobankerValue));
    }
  }
}
