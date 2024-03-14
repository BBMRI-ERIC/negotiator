import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.configuration.DevDatabaseConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
public class TestNegotiatorApplication {

  public static void main(String[] args) {
    SpringApplication.from(NegotiatorApplication::main)
        .with(DevDatabaseConfiguration.class)
        .run(args);
  }
}
