package eu.bbmri.eric.csit.service.negotiator.configuration;

import eu.bbmri.eric.csit.service.negotiator.repository.PersonRepository;
import java.time.Instant;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@Configuration
@EnableWebSecurity
@Profile({"test"})
public class TestSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired public NegotiatorUserDetailsService userDetailsService;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private DataSource dataSource;

  @Autowired public PersonRepository personRepository;

  @Bean
  JwtDecoder jwtDecoder() {
    return token -> {
      String userId = token.replace("Bearer ", "");
      String scopes = "";
      Instant iat;
      if ("researcher".equals(userId)) {
        scopes = "openid";
        iat = Instant.now();
      } else {
         iat = Instant.now().minusSeconds(24*3600);
      }
      return Jwt.withTokenValue("fake-token")
          .header("typ", "JWT")
          .header("alg", "none")
          .claim("oid", userId)
          .claim("scope", scopes)
          .claim("user_name", userId)
          .claim("iat", iat)
          .claim("exp", iat.plusSeconds(3600))
          .subject(userId)
          .build();
    };
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
    http.cors()
        .and()
        .csrf()
        .disable()
        .authorizeRequests()
        .antMatchers(HttpMethod.POST, "/v3/data-sources/**")
        .hasAuthority("ADMIN")
        .antMatchers(HttpMethod.PUT, "/v3/data-sources/**")
        .hasAuthority("ADMIN")
        .and()
        .authorizeRequests()
        .antMatchers("/v3/projects/**", "/v3/requests/**")
        .hasAuthority("RESEARCHER")
        .and()
        .authorizeRequests()
        .antMatchers(HttpMethod.POST, "/v3/queries/**")
        .hasAnyAuthority("ADMIN", "EXT_SERV")
        .and()
        .authorizeRequests()
        .antMatchers("/perun/**")
        .hasAuthority("PERUN_USER")
        .and()
        .authorizeRequests()
        .anyRequest()
        .permitAll()
        .and()
        .httpBasic()
        .and()
        .oauth2ResourceServer()
        .jwt()
        .jwtAuthenticationConverter(new NegotiatorJwtAuthenticationConverter(personRepository));
  }
}
