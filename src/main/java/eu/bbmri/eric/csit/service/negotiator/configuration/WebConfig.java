package eu.bbmri.eric.csit.service.negotiator.configuration;

import eu.bbmri.eric.csit.service.negotiator.converters.NegotiationEventConverter;
import eu.bbmri.eric.csit.service.negotiator.converters.NegotiationResourceEventConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addFormatters(FormatterRegistry registry) {
    // Register converter for NegotiationEvent enum
    registry.addConverter(new NegotiationEventConverter());

    // Register converter for NegotiationResourceEvent enum
    registry.addConverter(new NegotiationResourceEventConverter());
  }
}
