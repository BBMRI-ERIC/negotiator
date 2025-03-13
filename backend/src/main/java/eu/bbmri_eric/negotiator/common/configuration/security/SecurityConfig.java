package eu.bbmri_eric.negotiator.common.configuration.security;

import eu.bbmri_eric.negotiator.common.configuration.ExceptionHandlerFilter;
import eu.bbmri_eric.negotiator.common.configuration.security.oauth2.OAuth2Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Main Application Security configuration class. */
@Configuration
public class SecurityConfig {

  private final ExceptionHandlerFilter exceptionHandlerFilter;

  private final OAuth2Configuration oauthConfigurer;

  private final HTTPRegistryConfigurer httpRegistryConfigurer;

  private final AccessDeniedHandler accessDeniedHandler;

  public SecurityConfig(
      ExceptionHandlerFilter exceptionHandlerFilter,
      OAuth2Configuration oauthConfigurer,
      HTTPRegistryConfigurer httpRegistryConfigurer,
      AccessDeniedHandler accessDeniedHandler) {
    this.exceptionHandlerFilter = exceptionHandlerFilter;
    this.oauthConfigurer = oauthConfigurer;
    this.httpRegistryConfigurer = httpRegistryConfigurer;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.addFilterBefore(exceptionHandlerFilter, BearerTokenAuthenticationFilter.class)
        .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(httpRegistryConfigurer::configure)
        .oauth2ResourceServer(oauthConfigurer::configure);
    return http.build();
  }
}
