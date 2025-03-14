package eu.bbmri_eric.negotiator.common.configuration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.common.configuration.security.oauth2.CustomAccessDeniedHandler;
import eu.bbmri_eric.negotiator.common.configuration.security.oauth2.CustomBearerTokenAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
public class SecurityBeansConfig {
  @Bean
  MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
    return new MvcRequestMatcher.Builder(introspector);
  }

  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint() {
    return new CustomBearerTokenAuthenticationEntryPoint(new ObjectMapper());
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler() {
    return new CustomAccessDeniedHandler(new ObjectMapper());
  }
}
