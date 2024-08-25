package eu.bbmri_eric.negotiator.config;

import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresContainerManager {
  private static PostgreSQLContainer<?> container;

  public static PostgreSQLContainer<?> getContainer() {
    if (container == null) {
      container = new PostgreSQLContainer<>("postgres:16-alpine");
      container.start();
    }
    return container;
  }

  public static void stopContainer() {
    if (container != null) {
      container.stop();
      container = null;
    }
  }
}
