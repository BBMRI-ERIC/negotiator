package eu.bbmri_eric.negotiator;

import eu.bbmri_eric.negotiator.configuration.DevDatabaseConfiguration;
import org.springframework.boot.SpringApplication;

public class TestNegotiatorApplication {

  public static void main(String[] args) {
    SpringApplication.from(NegotiatorApplication::main)
        .with(DevDatabaseConfiguration.class)
        .run(args);
  }
}
