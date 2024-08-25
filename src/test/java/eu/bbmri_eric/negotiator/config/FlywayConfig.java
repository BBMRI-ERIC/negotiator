package eu.bbmri_eric.negotiator.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class FlywayConfig {

  @Bean
  public FlywayMigrationStrategy cleanMigrateStrategy() {
    return new FlywayMigrationStrategy() {
      @Override
      public void migrate(Flyway flyway) {
        // Clean the database before migration
        flyway.clean();

        // Perform migration
        flyway.migrate();
      }
    };
  }
}
