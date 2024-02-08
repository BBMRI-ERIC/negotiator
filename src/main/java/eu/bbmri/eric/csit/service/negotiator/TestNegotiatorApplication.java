package eu.bbmri.eric.csit.service.negotiator;

import eu.bbmri.eric.csit.service.negotiator.configuration.ContainerConfiguration;
import org.springframework.boot.SpringApplication;

public class TestNegotiatorApplication {

  public static void main(String[] args) {
    SpringApplication.from(NegotiatorApplication::main)
        .with(ContainerConfiguration.class)
        .run(args);
  }
}
