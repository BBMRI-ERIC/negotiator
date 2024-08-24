package eu.bbmri_eric.negotiator.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

@TestConfiguration
public class FlywayListener extends AbstractTestExecutionListener {

  @Override
  public void afterTestClass(TestContext testContext) throws Exception {
    super.beforeTestClass(testContext);
    var flyway = testContext.getApplicationContext().getBean(Flyway.class);
    flyway.clean();
    flyway.migrate();
  }
}
