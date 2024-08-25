package eu.bbmri_eric.negotiator.config;

import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/** Class containing configuration for the Postgres testcontainers. */
public class EnablePostgresTestContainerContextCustomizerFactory
    implements ContextCustomizerFactory {

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @Inherited
  @ActiveProfiles("test")
  @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
  @Import(FlywayConfig.class)
  public @interface EnabledPostgresTestContainer {}

  @Override
  public ContextCustomizer createContextCustomizer(
      @NonNull Class<?> testClass, @NonNull List<ContextConfigurationAttributes> configAttributes) {
    if (!(TestContextAnnotationUtils.hasAnnotation(
        testClass, EnabledPostgresTestContainer.class))) {
      return null;
    }
    RepositoryTest repositoryTestAnnotation =
        TestContextAnnotationUtils.findMergedAnnotation(testClass, RepositoryTest.class);
    IntegrationTest integrationTestAnnotation =
        TestContextAnnotationUtils.findMergedAnnotation(testClass, IntegrationTest.class);
    boolean loadData = shouldLoadData(repositoryTestAnnotation, integrationTestAnnotation);
    return new PostgresTestContainerContextCustomizer(loadData);
  }

  private static boolean shouldLoadData(
      RepositoryTest repositoryTestAnnotation, IntegrationTest integrationTestAnnotation) {
    boolean loadData = false;
    if (repositoryTestAnnotation == null) {
      if (integrationTestAnnotation != null) {
        loadData = integrationTestAnnotation.loadTestData();
      }
    }
    if (integrationTestAnnotation == null) {
      if (repositoryTestAnnotation != null) {
        loadData = repositoryTestAnnotation.loadTestData();
      }
    }
    return loadData;
  }

  @EqualsAndHashCode
  private static class PostgresTestContainerContextCustomizer implements ContextCustomizer {
    private final boolean loadTestData;

    public PostgresTestContainerContextCustomizer(boolean loadTestData) {
      this.loadTestData = loadTestData;
    }

    @Override
    public void customizeContext(
        @NonNull ConfigurableApplicationContext context,
        @NonNull MergedContextConfiguration mergedConfig) {
      var postgresContainer = PostgresContainerManager.getContainer();
      var properties = new HashMap<String, Object>();
      properties.put("spring.datasource.url", postgresContainer.getJdbcUrl());
      properties.put("spring.datasource.username", postgresContainer.getUsername());
      properties.put("spring.datasource.password", postgresContainer.getPassword());
      properties.put("spring.test.database.replace", "NONE");
      if (!loadTestData) {
        properties.put("spring.flyway.locations", "classpath:db/migration/");
      }
      var propertySource = new MapPropertySource("PostgresContainer Test Properties", properties);
      context.getEnvironment().getPropertySources().addFirst(propertySource);
    }
  }
}
