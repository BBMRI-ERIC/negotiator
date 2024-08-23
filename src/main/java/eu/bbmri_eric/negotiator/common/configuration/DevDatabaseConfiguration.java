package eu.bbmri_eric.negotiator.common.configuration;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
@Profile({"dev"})
public class DevDatabaseConfiguration {

  @Value("${spring.datasource.username}")
  private String databaseUsername;

  @Value("${spring.datasource.password}")
  private String databasePassword;

  @Value("${spring.datasource.database}")
  private String databaseName;

  @Bean
  @RestartScope
  @ServiceConnection
  public PostgreSQLContainer<?> postgresContainer() {
    PostgreSQLContainer<?> postgreSQLContainer =
        new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName(databaseName)
            .withUsername(databaseUsername)
            .withPassword(databasePassword);
    return postgreSQLContainer.withCreateContainerCmdModifier(
        cmd -> {
          Objects.requireNonNull(cmd.getHostConfig())
              .withPortBindings(
                  new PortBinding(Ports.Binding.bindPort(5432), new ExposedPort(5432)));
        });
  }
}
