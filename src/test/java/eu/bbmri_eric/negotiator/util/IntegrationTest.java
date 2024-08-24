package eu.bbmri_eric.negotiator.util;

import eu.bbmri_eric.negotiator.config.EnablePostgresTestContainerContextCustomizerFactory.EnabledPostgresTestContainer;
import eu.bbmri_eric.negotiator.config.FlywayListener;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;

/** Use this annotation for test classes that need the whole application context. */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Target(ElementType.TYPE)
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@EnabledPostgresTestContainer
@TestExecutionListeners(
    value = FlywayListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public @interface IntegrationTest {
  boolean loadTestData() default false;
}
