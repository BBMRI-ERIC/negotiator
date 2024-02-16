package eu.bbmri.eric.csit.service.negotiator;

import eu.bbmri.eric.csit.service.negotiator.configuration.DevDatabaseConfiguration;
import org.springframework.boot.SpringApplication;

public class TestNegotiatorApplication {

  public static void main(String[] args) {
    SpringApplication.from(NegotiatorApplication::main)
        .with(DevDatabaseConfiguration.class)
        .run(args);
  }
}
