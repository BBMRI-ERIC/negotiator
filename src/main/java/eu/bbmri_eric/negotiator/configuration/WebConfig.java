package eu.bbmri_eric.negotiator.configuration;

import eu.bbmri_eric.negotiator.converters.NegotiationEventConverter;
import eu.bbmri_eric.negotiator.converters.NegotiationResourceEventConverter;
import eu.bbmri_eric.negotiator.converters.NegotiationRoleConverter;
import java.util.List;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Value("#{'${spring.security.cors.allowed-methods}'.split(',')}")
  private List<String> ALLOWED_METHODS;
  @Value("#{'${spring.security.cors.allowed-origins}'.split(',')}")
  private List<String> ALLOWED_ORIGINS;
  @Value("#{'${spring.security.cors.allowed-headers}'.split(',')}")
  private List<String> ALLOWED_HEADERS;
  @Value("${spring.security.cors.allow-credentials}")
  private boolean ALLOW_CREDENTIALS;
  @Value("${spring.security.cors.max-age}")
  private Long MAX_AGE = 3600L;
  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new NegotiationEventConverter());
    registry.addConverter(new NegotiationRoleConverter());
    registry.addConverter(new NegotiationResourceEventConverter());
  }

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry.addMapping("/**").allowedOrigins(ALLOWED_ORIGINS.toArray(new String[0])).allowedMethods(ALLOWED_METHODS.toArray(new String[0])).allowedHeaders(ALLOWED_HEADERS.toArray(new String[0])).allowCredentials(ALLOW_CREDENTIALS).maxAge(MAX_AGE);
  }
}
