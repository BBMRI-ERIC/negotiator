package eu.bbmri_eric.negotiator.configuration;

import eu.bbmri_eric.negotiator.converters.NegotiationEventConverter;
import eu.bbmri_eric.negotiator.converters.NegotiationResourceEventConverter;
import eu.bbmri_eric.negotiator.converters.NegotiationRoleConverter;
import lombok.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new NegotiationEventConverter());
    registry.addConverter(new NegotiationRoleConverter());
    registry.addConverter(new NegotiationResourceEventConverter());
  }

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry.addMapping("/**").allowedOrigins("*");
  }
}
