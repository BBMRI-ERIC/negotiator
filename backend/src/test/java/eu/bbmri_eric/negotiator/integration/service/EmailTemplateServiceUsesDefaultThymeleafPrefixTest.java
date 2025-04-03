package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.notification.email.EmailTemplateService;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest(loadTestData = true)
@TestPropertySource(properties = "spring.thymeleaf.prefix=classpath:/templates/")
public class EmailTemplateServiceUsesDefaultThymeleafPrefixTest {

  @Autowired private EmailTemplateService emailTemplateService;

  @Test
  void updateNotificationTemplate_defaultTemplate_throwsForbiddenRequestException() {
    String templateName = "footer";
    String newTemplateContent = "<html><body>Updated Template</body></html>";

    assertThrows(
        ForbiddenRequestException.class,
        () -> emailTemplateService.updateNotificationTemplate(templateName, newTemplateContent));
  }

  @Test
  void resetNotificationTemplate_defaultTemplate_throwsForbiddenRequestException() {
    String templateName = "footer";

    assertThrows(
        ForbiddenRequestException.class,
        () -> emailTemplateService.resetNotificationTemplate(templateName));
  }
}
