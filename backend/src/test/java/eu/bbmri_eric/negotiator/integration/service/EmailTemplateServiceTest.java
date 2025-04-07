package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.notification.email.EmailTemplateService;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;

@IntegrationTest(loadTestData = true)
public class EmailTemplateServiceTest {
  @Autowired EmailTemplateService emailTemplateService;

  @Test
  void getNotificationTemplate_existingTemplate_ok() {
    String templateName = "footer";
    String templateContent = emailTemplateService.getNotificationTemplate(templateName);
    assertNotNull(templateContent);
    assertFalse(templateContent.isEmpty());
  }

  @Test
  void getNotificationTemplate_nonExistentTemplate_throwsEntityNotFoundException() {
    String templateName = "nonExistentTemplate";
    assertThrows(
        EntityNotFoundException.class,
        () -> emailTemplateService.getNotificationTemplate(templateName));
  }

  @Test
  void updateNotificationTemplate_validTemplate_ok() {
    String templateName = "footer";
    String newTemplateContent = "<html><body>Updated Template</body></html>";
    String updatedTemplate =
        emailTemplateService.updateNotificationTemplate(templateName, newTemplateContent);
    assertEquals(Jsoup.parse(newTemplateContent).html(), updatedTemplate);
  }

  @Test
  void updateNotificationTemplate_invalidHtml_throwsUnsupportedMediaTypeException() {
    String templateName = "footer";
    String invalidTemplateContent = null;
    assertThrows(
        UnsupportedMediaTypeException.class,
        () ->
            emailTemplateService.updateNotificationTemplate(templateName, invalidTemplateContent));
  }

  @Test
  void updateNotificationTemplate_nonExistentDefaultTemplate_throwsForbiddenRequestException() {
    String templateName = "templateWithoutDefault";
    String newTemplateContent = "<html><body>Updated Template</body></html>";
    assertThrows(
        ForbiddenRequestException.class,
        () -> emailTemplateService.updateNotificationTemplate(templateName, newTemplateContent));
  }

  @Test
  void updateNotificationTemplate_invalidPathToOtherLocation_throwsForbiddenRequestException() {
    String templateName = "../path/to/other/location";
    String newTemplateContent = "<html><body>Updated Template</body></html>";
    assertThrows(
        ForbiddenRequestException.class,
        () -> emailTemplateService.updateNotificationTemplate(templateName, newTemplateContent));
  }

  @Test
  void resetNotificationTemplate_existingDefaultTemplate_ok() {
    String templateName = "footer";
    String defaultTemplateContent = emailTemplateService.resetNotificationTemplate(templateName);
    assertNotNull(defaultTemplateContent);
    assertFalse(defaultTemplateContent.isEmpty());
  }

  @Test
  void resetNotificationTemplate_nonExistentDefaultTemplate_throwsEntityNotFoundException() {
    String templateName = "templateWithoutDefault";
    assertThrows(
        EntityNotFoundException.class,
        () -> emailTemplateService.resetNotificationTemplate(templateName));
  }

  @Test
  void getAllNotificationTemplates_existingTemplates_ok() {
    List<String> templates = emailTemplateService.getAllNotificationTemplates();
    assertNotNull(templates);
    assertFalse(templates.isEmpty());
  }
}
