package eu.bbmri_eric.negotiator.config;

import org.testcontainers.containers.PostgreSQLContainer;

/** Class for managing the Postgres container used in testing. */
public class PostgresContainerManager {
  private static PostgreSQLContainer<?> container;

  public static PostgreSQLContainer<?> getContainer() {
    if (container == null) {
      container = new PostgreSQLContainer<>("postgres:16-alpine");
      // The container should be automatically stopped
      // when the Testcontainers orchestration container is terminated by Junit.
      container.start();
    }
    return container;
  }
}
