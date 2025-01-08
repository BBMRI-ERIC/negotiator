package eu.bbmri_eric.negotiator.util;

import eu.bbmri_eric.negotiator.config.EnablePostgresTestContainerContextCustomizerFactory.EnabledPostgresTestContainer;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

/** Use this annotation for test classes that need the whole application context. */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Target(ElementType.TYPE)
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@EnabledPostgresTestContainer
public @interface IntegrationTest {
  boolean loadTestData() default false;
}
