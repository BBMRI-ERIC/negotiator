package eu.bbmri_eric.negotiator.common.configuration.security;

import eu.bbmri_eric.negotiator.common.configuration.ExceptionHandlerFilter;
import eu.bbmri_eric.negotiator.common.configuration.security.oauth2.OAuth2Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;

/** Main Application Security configuration class. */
@Configuration
public class SecurityConfig {

  private final ExceptionHandlerFilter exceptionHandlerFilter;

  private final OAuth2Configuration oauthConfigurer;

  private final HTTPRegistryConfigurer httpRegistryConfigurer;

  public SecurityConfig(
      ExceptionHandlerFilter exceptionHandlerFilter,
      OAuth2Configuration oauthConfigurer,
      HTTPRegistryConfigurer httpRegistryConfigurer) {
    this.exceptionHandlerFilter = exceptionHandlerFilter;
    this.oauthConfigurer = oauthConfigurer;
    this.httpRegistryConfigurer = httpRegistryConfigurer;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc)
      throws Exception {
    http.addFilterBefore(exceptionHandlerFilter, BearerTokenAuthenticationFilter.class)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(httpRegistryConfigurer::configure)
        .oauth2ResourceServer(oauthConfigurer::configure);
    return http.build();
  }
}
