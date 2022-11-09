package eu.bbmri.eric.csit.service.negotiator.configuration;

import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorJwtAuthenticationConverter;
import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetailsService;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@Profile({"dev", "prod", "docker"})
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired public NegotiatorUserDetailsService userDetailsService;

  @Autowired public DataSource dataSource;

  @Autowired public PasswordEncoder passwordEncoder;

  @Autowired public PersonRepository personRepository;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String jwtIssuer;

  @Value("${negotiator.authorization.claim}")
  private String authzClaim;

  @Value("${negotiator.authorization.subjectClaim}")
  private String authzSubjectClaim;

  @Value("${negotiator.authorization.adminClaimValue}")
  private String authzAdminValue;

  @Value("${negotiator.authorization.researcherClaimValue}")
  private String authzResearcherValue;

  @Value("${negotiator.authorization.biobankerClaimValue}")
  private String authzBiobankerValue;

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
        .and()
        .csrf()
        .disable()
        .headers()
        .frameOptions()
        .disable();

    http.authorizeRequests()
        .antMatchers(HttpMethod.POST, "/v3/data-sources/**")
        .hasAuthority("ADMIN")
        .and()
        .authorizeRequests()
        .antMatchers("/v3/projects/**", "/v3/negotiations/**")
        .hasAuthority("RESEARCHER")
        .and()
        .authorizeRequests()
        .antMatchers(HttpMethod.POST, "/v3/requests/**")
        .hasAnyAuthority("ADMIN", "EXT_SERV")
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
              new NegotiatorJwtAuthenticationConverter(
                  personRepository,
                  authzClaim,
                  authzSubjectClaim,
                  authzAdminValue,
                  authzResearcherValue,
                  authzBiobankerValue));
    }
  }
}
