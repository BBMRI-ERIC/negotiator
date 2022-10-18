package eu.bbmri.eric.csit.service.negotiator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"eu.bbmri.eric.csit.service.negotiator.*"})
@EnableJpaRepositories(basePackages = {"eu.bbmri.eric.csit.service.negotiator.*"})
@EnableAutoConfiguration
public class NegotiatorApplication {

  public static void main(String[] args) {
    SpringApplication.run(NegotiatorApplication.class, args);
  }
}
