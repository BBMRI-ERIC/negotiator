package eu.bbmri_eric.negotiator.config;

import lombok.NonNull;
import org.flywaydb.core.Flyway;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class FlywayListener extends AbstractTestExecutionListener {

  @Override
  public void beforeTestClass(@NonNull TestContext testContext) throws Exception {
    super.afterTestClass(testContext);
    Flyway flyway = testContext.getApplicationContext().getBean(Flyway.class);
    flyway.clean();
    flyway.migrate();
  }
}
