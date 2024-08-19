package eu.bbmri_eric.negotiator.configuration;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class ServletInitializer extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(NegotiatorApplication.class);
  }
}
